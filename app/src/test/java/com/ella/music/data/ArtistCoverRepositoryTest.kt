package com.ella.music.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistCoverRepositoryTest {
    @Test
    fun imageExtensionsMatchArtistNamesCaseInsensitively() {
        assertEquals(
            "fleetwood mac",
            artistCoverMatchKey("Fleetwood Mac.JPG")
        )
        assertEquals(
            "taylor swift",
            artistCoverMatchKey("Taylor   Swift.webp")
        )
    }

    @Test
    fun unsupportedFilesAreIgnored() {
        assertNull(artistCoverMatchKey("Fleetwood Mac.txt"))
        assertNull(artistCoverMatchKey("README"))
    }

    @Test
    fun normalizeArtistCoverKeyCleansWhitespace() {
        assertEquals(
            "lana del rey",
            normalizeArtistCoverKey("  Lana   Del Rey  ")
        )
    }
}
