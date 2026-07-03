package com.ella.music.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.ella.music.data.model.Song
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataPatchSnapshotPolicyTest {
    @Test
    fun metadataPatchForCurrentSongIsDisplayOnly() {
        val song = song(id = 1L, path = "/music/current.flac")

        assertTrue(
            isDisplayOnlyMetadataPatchSnapshot(
                isMetadataOnlyPatch = true,
                snapshotSong = song.copy(title = "Lyric display title"),
                currentSong = song
            )
        )
    }

    @Test
    fun metadataPatchForDifferentSongStillUpdatesCurrentSong() {
        assertFalse(
            isDisplayOnlyMetadataPatchSnapshot(
                isMetadataOnlyPatch = true,
                snapshotSong = song(id = 2L, path = "/music/next.flac"),
                currentSong = song(id = 1L, path = "/music/current.flac")
            )
        )
    }

    @Test
    fun unmarkedSnapshotStillUsesNormalSongRefreshPath() {
        val song = song(id = 1L, path = "/music/current.flac")

        assertFalse(
            isDisplayOnlyMetadataPatchSnapshot(
                isMetadataOnlyPatch = false,
                snapshotSong = song,
                currentSong = song
            )
        )
    }

    @Test
    fun sourceUpdateForMetadataPatchIsIgnored() {
        val song = song(id = 1L, path = "/music/current.flac")
        val item = mediaItem(song, markAsPatch = true)

        assertTrue(
            shouldIgnoreDisplayOnlyTimelineUpdate(
                reason = Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE,
                currentItem = item,
                currentSong = song
            )
        )
    }

    @Test
    fun sourceUpdateForSameSongWithoutPatchMarkerIsStillIgnored() {
        val song = song(id = 1L, path = "/music/current.flac")
        val item = mediaItem(song, markAsPatch = false)

        assertTrue(
            shouldIgnoreDisplayOnlyTimelineUpdate(
                reason = Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE,
                currentItem = item,
                currentSong = song
            )
        )
    }

    @Test
    fun playlistChangeForDifferentSongStillRefreshesNormally() {
        val currentSong = song(id = 1L, path = "/music/current.flac")
        val nextSong = song(id = 2L, path = "/music/next.flac")
        val item = mediaItem(nextSong, markAsPatch = false)

        assertFalse(
            shouldIgnoreDisplayOnlyTimelineUpdate(
                reason = Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED,
                currentItem = item,
                currentSong = currentSong
            )
        )
        assertFalse(
            shouldIgnoreDisplayOnlyTimelineUpdate(
                reason = Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE,
                currentItem = item,
                currentSong = currentSong
            )
        )
    }

    private fun mediaItem(song: Song, markAsPatch: Boolean): MediaItem {
        val extras = song.toMediaItemExtras()
        if (markAsPatch) {
            extras.markMetadataOnlyPatch(PATCH_REASON_BLUETOOTH_LYRIC)
        }
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    private fun song(id: Long, path: String): Song = Song(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        albumId = 1L,
        duration = 180_000L,
        path = path,
        fileName = path.substringAfterLast('/')
    )
}
