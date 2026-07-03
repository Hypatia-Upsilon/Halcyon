package com.ella.music.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.ella.music.data.model.Song

internal fun isDisplayOnlyMetadataPatchSnapshot(
    isMetadataOnlyPatch: Boolean,
    snapshotSong: Song?,
    currentSong: Song?
): Boolean {
    return isMetadataOnlyPatch &&
        snapshotSong != null &&
        snapshotSong.isSamePlaybackIdentity(currentSong)
}

internal fun shouldIgnoreDisplayOnlyTimelineUpdate(
    reason: Int,
    currentItem: MediaItem?,
    currentSong: Song?
): Boolean {
    if (reason != Player.TIMELINE_CHANGE_REASON_SOURCE_UPDATE) return false
    if (currentItem?.isMetadataOnlyPatch() == true) return true
    val itemSong = currentItem?.toSongFromMediaItemExtras() ?: return false
    return itemSong.isSamePlaybackIdentity(currentSong)
}
