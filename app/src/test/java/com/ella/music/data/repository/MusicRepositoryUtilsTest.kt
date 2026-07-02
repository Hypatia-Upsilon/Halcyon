package com.ella.music.data.repository

import com.ella.music.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicRepositoryUtilsTest {
    @Test
    fun ac4AjocM4aIsNotMisidentifiedAsAac() {
        val song = song(
            fileName = "07. The Chain (Dolby Atmos AC-4 A-JOC).m4a",
            album = "Rumours (Dolby Atmos AC-4 A-JOC)"
        )

        assertEquals("AC4 A-JOC", song.audioFormatLabel("audio/ac4") { 448_000 })
    }

    @Test
    fun ac4ImmersiveStereoM4aKeepsItsVariant() {
        val song = song(
            fileName = "07. The Chain (Dolby Atmos Immersive Stereo).m4a",
            album = "Rumours (Dolby Atmos Immersive Stereo)"
        )

        assertEquals("AC4 Immersive Stereo", song.audioFormatLabel("audio/ac4") { 256_000 })
    }

    private fun song(fileName: String, album: String): Song = Song(
        id = 7L,
        title = "The Chain",
        artist = "Fleetwood Mac",
        album = album,
        albumId = 1L,
        duration = 267_000L,
        path = "C:/$fileName",
        fileName = fileName,
        mimeType = "audio/mp4"
    )
}
