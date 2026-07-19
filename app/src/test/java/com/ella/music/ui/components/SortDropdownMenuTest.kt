package com.ella.music.ui.components

import com.ella.music.ui.listmodel.SortDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SortDropdownMenuTest {
    @Test
    fun pairedModesShareOneRowAndApplyTheRequestedDirection() {
        var selected = TestSortMode.YearDescending
        val items = directionalSortModeDropdownItems(
            fields = listOf(
                DirectionalSortModeField(
                    text = "Year",
                    ascendingMode = TestSortMode.YearAscending,
                    descendingMode = TestSortMode.YearDescending
                ),
                DirectionalSortModeField(
                    text = "Duration",
                    descendingMode = TestSortMode.DurationDescending
                )
            ),
            selectedMode = selected,
            onSelect = { selected = it }
        )

        assertEquals(2, items.size)
        assertTrue(items[0].selected)
        assertEquals(SortDirection.Descending, items[0].direction)
        assertNotNull(items[0].onSelectAscending)
        assertNotNull(items[0].onSelectDescending)
        assertNotNull(items[1].onSelectDescending)
        assertEquals(null, items[1].onSelectAscending)

        items[0].onSelectAscending?.invoke()
        assertEquals(TestSortMode.YearAscending, selected)
    }

    @Test
    fun unavailableDirectionStaysDisabledInsteadOfChangingSortSemantics() {
        var selected = TestSortMode.DurationDescending
        val item = directionalSortModeDropdownItems(
            fields = listOf(
                DirectionalSortModeField(
                    text = "Duration",
                    descendingMode = TestSortMode.DurationDescending
                )
            ),
            selectedMode = selected,
            onSelect = { selected = it }
        ).single()

        assertTrue(item.selected)
        assertEquals(SortDirection.Descending, item.direction)
        assertFalse(item.onSelectAscending != null)
        item.onClick()
        assertEquals(TestSortMode.DurationDescending, selected)
    }

    private enum class TestSortMode {
        YearAscending,
        YearDescending,
        DurationDescending
    }
}
