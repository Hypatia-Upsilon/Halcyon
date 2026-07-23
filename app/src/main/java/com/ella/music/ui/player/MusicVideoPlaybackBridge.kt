package com.ella.music.ui.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * The MV surface owns a second, muted ExoPlayer.  Keep its clock available to the player controls
 * instead of accidentally seeking the audio player while the MV is visible.
 */
internal data class MusicVideoPlaybackSnapshot(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playWhenReady: Boolean = false
)

internal object MusicVideoPlaybackBridge {
    private data class Entry(
        val snapshot: MutableStateFlow<MusicVideoPlaybackSnapshot> =
            MutableStateFlow(MusicVideoPlaybackSnapshot()),
        @Volatile var playWhenReady: Boolean = false,
        @Volatile var player: Player? = null
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    private fun keyFor(source: DynamicCoverSource): String =
        source.playbackOwnerKey.ifBlank { source.failureKey }

    fun snapshot(source: DynamicCoverSource?): StateFlow<MusicVideoPlaybackSnapshot> {
        val key = source?.let(::keyFor).orEmpty()
        return entries.getOrPut(key) { Entry() }.snapshot
    }

    fun attach(source: DynamicCoverSource, player: Player) {
        if (source.role != PlayerVideoRole.MusicVideo) return
        val entry = entries.getOrPut(keyFor(source)) { Entry() }
        entry.player = player
        player.playWhenReady = entry.playWhenReady
        publish(source, player)
    }

    fun publish(source: DynamicCoverSource, player: Player) {
        if (source.role != PlayerVideoRole.MusicVideo) return
        val duration = player.duration.takeIf { it > 0L } ?: 0L
        val entry = entries.getOrPut(keyFor(source)) { Entry() }
        entry.playWhenReady = player.playWhenReady
        entry.snapshot.value = MusicVideoPlaybackSnapshot(
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            playWhenReady = entry.playWhenReady
        )
    }

    fun detach(source: DynamicCoverSource, player: Player) {
        if (source.role != PlayerVideoRole.MusicVideo) return
        entries[keyFor(source)]?.takeIf { it.player === player }?.player = null
    }

    fun seekToProgress(source: DynamicCoverSource?, progress: Float) {
        val resolvedSource = source ?: return
        val entry = entries[keyFor(resolvedSource)] ?: return
        val player = entry.player ?: return
        val duration = player.duration.takeIf { it > 0L } ?: entry.snapshot.value.durationMs
        if (duration <= 0L) return
        player.seekTo((duration * progress.coerceIn(0f, 1f)).toLong())
        publish(resolvedSource, player)
    }

    fun setPlaying(source: DynamicCoverSource?, playing: Boolean) {
        val resolvedSource = source ?: return
        val entry = entries.getOrPut(keyFor(resolvedSource)) { Entry() }
        entry.playWhenReady = playing
        entry.player?.let { player ->
            player.playWhenReady = playing
            publish(resolvedSource, player)
        } ?: run {
            entry.snapshot.value = entry.snapshot.value.copy(playWhenReady = playing)
        }
    }

    fun togglePlayback(source: DynamicCoverSource?) {
        val resolvedSource = source ?: return
        val entry = entries.getOrPut(keyFor(resolvedSource)) { Entry() }
        setPlaying(resolvedSource, !entry.playWhenReady)
    }
}
