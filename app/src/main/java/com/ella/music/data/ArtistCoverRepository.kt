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
    private var cachedIndex: Map<String, ArtistCoverAsset> = emptyMap()
    private val indexLock = Any()

    fun getArtistCoverUri(
        artistName: String,
        folderLocation: String
    ): Uri? {
        return getArtistCoverAsset(artistName, folderLocation)
            ?.takeIf { it.kind == ArtistCoverKind.Image }
            ?.uri
    }

    fun getArtistCoverAsset(
        artistName: String,
        folderLocation: String
    ): ArtistCoverAsset? {
        val artistKey = normalizeArtistCoverKey(artistName)
        val safeFolderLocation = folderLocation.trim()
        if (artistKey.isBlank() || safeFolderLocation.isBlank()) return null
        return ensureIndex(safeFolderLocation)[artistKey]
    }

    private fun ensureIndex(folderLocation: String): Map<String, ArtistCoverAsset> {
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

    private fun buildIndex(folderLocation: String): Map<String, ArtistCoverAsset> {
        return when {
            folderLocation.startsWith("content://", ignoreCase = true) -> {
                val root = DocumentFile.fromTreeUri(context, Uri.parse(folderLocation)) ?: return emptyMap()
                buildTreeIndex(root)
            }

            else -> buildLocalFolderIndex(File(folderLocation))
        }
    }

    private fun buildTreeIndex(root: DocumentFile): Map<String, ArtistCoverAsset> {
        val index = linkedMapOf<String, ArtistCoverAsset>()

        fun visit(node: DocumentFile) {
            val children = runCatching { node.listFiles() }.getOrElse { emptyArray() }
            children.forEach { child ->
                when {
                    child.isDirectory -> visit(child)
                    child.isFile -> {
                        val match = artistCoverMatch(child.name.orEmpty(), child.type) ?: return@forEach
                        index.putPreferredArtistCover(match.key, ArtistCoverAsset(child.uri, match.kind))
                    }
                }
            }
        }

        visit(root)
        return index
    }

    private fun buildLocalFolderIndex(root: File): Map<String, ArtistCoverAsset> {
        if (!root.exists() || !root.isDirectory) return emptyMap()
        val index = linkedMapOf<String, ArtistCoverAsset>()
        runCatching {
            root.walkTopDown().forEach { file ->
                if (!file.isFile) return@forEach
                val match = artistCoverMatch(file.name) ?: return@forEach
                index.putPreferredArtistCover(match.key, ArtistCoverAsset(Uri.fromFile(file), match.kind))
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

data class ArtistCoverAsset(
    val uri: Uri,
    val kind: ArtistCoverKind
)

enum class ArtistCoverKind {
    Image,
    Video
}

internal data class ArtistCoverMatch(
    val key: String,
    val kind: ArtistCoverKind
)

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

private val supportedArtistCoverVideoExtensions = setOf(
    "mp4",
    "m4v",
    "mov",
    "webm",
    "mkv"
)

private fun MutableMap<String, ArtistCoverAsset>.putPreferredArtistCover(
    key: String,
    candidate: ArtistCoverAsset
) {
    val existing = this[key]
    if (existing == null || (existing.kind == ArtistCoverKind.Image && candidate.kind == ArtistCoverKind.Video)) {
        this[key] = candidate
    }
}

internal fun artistCoverMatchKey(
    fileName: String,
    mimeType: String? = null
): String? {
    return artistCoverMatch(fileName, mimeType)?.key
}

internal fun artistCoverMatch(
    fileName: String,
    mimeType: String? = null
): ArtistCoverMatch? {
    val trimmedName = fileName.trim()
    if (trimmedName.isBlank()) return null
    val extension = trimmedName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val kind = when {
        mimeType?.startsWith("video/", ignoreCase = true) == true ||
            extension in supportedArtistCoverVideoExtensions -> ArtistCoverKind.Video
        mimeType?.startsWith("image/", ignoreCase = true) == true ||
            extension in supportedArtistCoverExtensions -> ArtistCoverKind.Image
        else -> return null
    }
    val baseName = trimmedName.substringBeforeLast('.', trimmedName)
    val key = normalizeArtistCoverKey(baseName).takeIf { it.isNotBlank() } ?: return null
    return ArtistCoverMatch(key = key, kind = kind)
}

internal fun normalizeArtistCoverKey(value: String): String {
    return LibraryNormalizer.cleanedArtistText(value)
        .replace(Regex("""\s+"""), " ")
        .trim()
        .lowercase(Locale.ROOT)
}
