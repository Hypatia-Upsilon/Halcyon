package com.ella.music.viewmodel

import com.ella.music.data.PlaybackStatsStore
import com.ella.music.data.model.Song

internal class PlayerPlaybackStatsTracker(
    private val playbackStatsStore: PlaybackStatsStore,
    private val minPlaybackStatsListenMs: Long = 20_000L,
    private val onPlayCounted: (Song) -> Unit = {},
    private val onLastFmScrobbleEligible: (Song, Long) -> Unit = { _, _ -> }
) {
    private var statsSongId: Long? = null
    private var statsSong: Song? = null
    private var playCountedSongId: Long? = null
    private var scrobbleQueuedSongId: Long? = null
    private var pendingListenMs = 0L
    private var lastFmListenMs = 0L
    private var lastStatsTickMs = 0L
    private var songStartedAtWallClockMs = 0L

    suspend fun update(
        nowMs: Long,
        song: Song?,
        isPlaying: Boolean
    ) {
        val songId = song?.id

        if (songId != statsSongId) {
            flush()
            statsSongId = songId
            statsSong = song
            playCountedSongId = null
            scrobbleQueuedSongId = null
            lastFmListenMs = 0L
            songStartedAtWallClockMs = if (song == null) 0L else System.currentTimeMillis()
            lastStatsTickMs = nowMs
            return
        }

        if (song != null && isPlaying) {
            val elapsedMs = if (lastStatsTickMs > 0L) {
                (nowMs - lastStatsTickMs).coerceIn(0L, 1500L)
            } else {
                0L
            }
            if (lastStatsTickMs > 0L) {
                pendingListenMs += elapsedMs
                lastFmListenMs += elapsedMs
            }
            if (playCountedSongId != song.id && pendingListenMs >= minPlaybackStatsListenMs) {
                playbackStatsStore.recordPlay(song)
                onPlayCounted(song)
                playCountedSongId = song.id
            }
            if (
                scrobbleQueuedSongId != song.id &&
                lastFmListenMs >= song.lastFmScrobbleThresholdMs()
            ) {
                onLastFmScrobbleEligible(song, songStartedAtWallClockMs)
                scrobbleQueuedSongId = song.id
            }
            if (playCountedSongId == song.id && pendingListenMs >= 5000L) {
                playbackStatsStore.addListenTime(song, pendingListenMs)
                pendingListenMs = 0L
            }
        } else {
            flush()
        }
        lastStatsTickMs = nowMs
    }

    fun takePendingFlush(): PlayerPlaybackStatsPendingFlush? {
        val song = statsSong
        val listenedMs = pendingListenMs
        pendingListenMs = 0L
        return if (song != null && playCountedSongId == song.id && listenedMs > 0L) {
            PlayerPlaybackStatsPendingFlush(song, listenedMs)
        } else {
            null
        }
    }

    private suspend fun flush() {
        val song = statsSong
        if (song != null && playCountedSongId == song.id && pendingListenMs > 0L) {
            playbackStatsStore.addListenTime(song, pendingListenMs)
        }
        pendingListenMs = 0L
    }
}

/** Last.fm accepts a track after 50% or four minutes, whichever comes first; sub-30s tracks skip it. */
private fun Song.lastFmScrobbleThresholdMs(): Long = when {
    duration in 1L..30_000L -> Long.MAX_VALUE
    duration > 30_000L -> minOf(duration / 2L, 240_000L)
    else -> 240_000L
}

internal data class PlayerPlaybackStatsPendingFlush(
    val song: Song,
    val listenedMs: Long
)
