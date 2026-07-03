package com.ella.music.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.Locale

class ArtistCoverRepository private constructor(
    private val context: Context
) {
    @Volatile
    private var cachedFolderLocation: String? = null
    @Volatile
    private var cachedIndex: Map<String, String> = emptyMap()
    private val indexLock = Any()

    fun getArtistCoverUri(
        artistName: String,
        folderLocation: String
    ): Uri? {
        val artistKey = normalizeArtistCoverKey(artistName)
        val safeFolderLocation = folderLocation.trim()
        if (artistKey.isBlank() || safeFolderLocation.isBlank()) return null
        val uriString = ensureIndex(safeFolderLocation)[artistKey] ?: return null
        return Uri.parse(uriString)
    }

    private fun ensureIndex(folderLocation: String): Map<String, String> {
        cachedFolderLocation
            ?.takeIf { it == folderLocation }
            ?.let { return cachedIndex }

        synchronized(indexLock) {
            cachedFolderLocation
                ?.takeIf { it == folderLocation }
                ?.let { return cachedIndex }

            val built = buildIndex(folderLocation)
            cachedFolderLocation = folderLocation
            cachedIndex = built
            return built
        }
    }

    private fun buildIndex(folderLocation: String): Map<String, String> {
        return when {
            folderLocation.startsWith("content://", ignoreCase = true) -> {
                val root = DocumentFile.fromTreeUri(context, Uri.parse(folderLocation)) ?: return emptyMap()
                buildTreeIndex(root)
            }

            else -> buildLocalFolderIndex(File(folderLocation))
        }
    }

    private fun buildTreeIndex(root: DocumentFile): Map<String, String> {
        val index = linkedMapOf<String, String>()

        fun visit(node: DocumentFile) {
            val children = runCatching { node.listFiles() }.getOrElse { emptyArray() }
            children.forEach { child ->
                when {
                    child.isDirectory -> visit(child)
                    child.isFile -> {
                        val key = artistCoverMatchKey(child.name.orEmpty(), child.type) ?: return@forEach
                        index.putIfAbsent(key, child.uri.toString())
                    }
                }
            }
        }

        visit(root)
        return index
    }

    private fun buildLocalFolderIndex(root: File): Map<String, String> {
        if (!root.exists() || !root.isDirectory) return emptyMap()
        val index = linkedMapOf<String, String>()
        runCatching {
            root.walkTopDown().forEach { file ->
                if (!file.isFile) return@forEach
                val key = artistCoverMatchKey(file.name) ?: return@forEach
                index.putIfAbsent(key, Uri.fromFile(file).toString())
            }
        }
        return index
    }

    companion object {
        @Volatile
        private var instance: ArtistCoverRepository? = null

        fun getInstance(context: Context): ArtistCoverRepository {
            return instance ?: synchronized(this) {
                instance ?: ArtistCoverRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

private val supportedArtistCoverExtensions = setOf(
    "jpg",
    "jpeg",
    "png",
    "webp",
    "bmp",
    "gif",
    "avif",
    "heic",
    "heif"
)

internal fun artistCoverMatchKey(
    fileName: String,
    mimeType: String? = null
): String? {
    val trimmedName = fileName.trim()
    if (trimmedName.isBlank()) return null
    val extension = trimmedName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val isSupportedImage = mimeType?.startsWith("image/", ignoreCase = true) == true ||
        extension in supportedArtistCoverExtensions
    if (!isSupportedImage) return null
    val baseName = trimmedName.substringBeforeLast('.', trimmedName)
    return normalizeArtistCoverKey(baseName).takeIf { it.isNotBlank() }
}

internal fun normalizeArtistCoverKey(value: String): String {
    return LibraryNormalizer.cleanedArtistText(value)
        .replace(Regex("""\s+"""), " ")
        .trim()
        .lowercase(Locale.ROOT)
}
