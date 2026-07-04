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
    fun videoExtensionsAlsoMatchArtistNames() {
        assertEquals(
            "fleetwood mac",
            artistCoverMatchKey("Fleetwood Mac.mp4")
        )
        assertEquals(
            ArtistCoverKind.Video,
            artistCoverMatch("Taylor Swift.webm")?.kind
        )
    }

    @Test
    fun supportedVideoMimeTypesAreRecognized() {
        val match = artistCoverMatch("Aimer.cover", "video/mp4")
        assertEquals("aimer", match?.key)
        assertEquals(ArtistCoverKind.Video, match?.kind)
    }

    @Test
    fun normalizeArtistCoverKeyCleansWhitespace() {
        assertEquals(
            "lana del rey",
            normalizeArtistCoverKey("  Lana   Del Rey  ")
        )
    }
}
