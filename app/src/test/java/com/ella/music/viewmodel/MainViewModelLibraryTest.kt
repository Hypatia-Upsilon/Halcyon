package com.ella.music.viewmodel

import com.ella.music.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class MainViewModelLibraryTest {
    @Test
    fun filterSongsForArtist_ignoresAlbumArtistWhenDisabled() {
        val songs = listOf(
            song(id = 1, artist = "Track Artist", albumArtist = "Album Artist"),
            song(id = 2, artist = "Album Artist", albumArtist = "")
        )

        val result = filterSongsForArtist(
            songs = songs,
            artistName = "Album Artist",
            includeAlbumArtist = false
        )

        assertEquals(listOf(2L), result.map { it.id })
    }

    @Test
    fun filterSongsForArtist_matchesAlbumArtistWhenEnabled() {
        val songs = listOf(
            song(id = 1, artist = "Track Artist", albumArtist = "Album Artist"),
            song(id = 2, artist = "Other Artist", albumArtist = "")
        )

        val result = filterSongsForArtist(
            songs = songs,
            artistName = "Album Artist",
            includeAlbumArtist = true
        )

        assertEquals(listOf(1L), result.map { it.id })
    }

    private fun song(
        id: Long,
        artist: String,
        albumArtist: String
    ): Song = Song(
        id = id,
        title = "Song $id",
        artist = artist,
        album = "Album",
        albumId = id,
        duration = 180_000L,
        path = "/music/$id.flac",
        fileName = "$id.flac",
        albumArtist = albumArtist
    )
}
