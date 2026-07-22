package com.ella.music.ui.components

import com.ella.music.data.model.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Test

class MiniPlayerLyricTimingTest {
    @Test
    fun usesTheLastWordEndBeforeCompletingTheLine() {
        val timing = MiniPlayerLyricTiming(
            lineStartMs = 1_000L,
            lineEndMs = 1_800L,
            words = listOf(
                LyricWord("long", 1_000L, 1_400L),
                LyricWord("note", 1_400L, 2_000L)
            )
        )

        assertEquals(0f, timing.progressAt(1_000L), 0.0001f)
        assertEquals(0.5f, timing.progressAt(1_500L), 0.0001f)
        assertEquals(1f, timing.progressAt(2_000L), 0.0001f)
    }
}
