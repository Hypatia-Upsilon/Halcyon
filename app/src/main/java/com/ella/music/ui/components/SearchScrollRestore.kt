package com.ella.music.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

/**
 * Restores the place a user was reading after an inline search is dismissed.
 *
 * Filtering a lazy list can legitimately move its state back to item zero when the old visible
 * item is absent from the filtered result.  Remembering the pre-search index means closing the
 * search returns to the original unfiltered position instead of looking like a page refresh.
 */
@Composable
internal fun RestoreListScrollAfterSearch(
    searchExpanded: Boolean,
    query: String,
    listState: LazyListState
) {
    var wasSearchExpanded by remember { mutableStateOf(searchExpanded) }
    var anchor by remember { mutableStateOf<ScrollAnchor?>(null) }
    LaunchedEffect(searchExpanded, query) {
        when {
            searchExpanded && !wasSearchExpanded -> {
                anchor = ScrollAnchor(
                    index = listState.firstVisibleItemIndex,
                    offset = listState.firstVisibleItemScrollOffset
                )
            }

            !searchExpanded && wasSearchExpanded && query.isBlank() -> {
                anchor?.let { saved ->
                    // Wait until the cleared query has restored the original items.
                    withFrameNanos { }
                    val lastIndex = listState.layoutInfo.totalItemsCount - 1
                    if (lastIndex >= 0) {
                        listState.scrollToItem(saved.index.coerceIn(0, lastIndex), saved.offset)
                    }
                }
                anchor = null
            }
        }
        wasSearchExpanded = searchExpanded
    }
}

@Composable
internal fun RestoreGridScrollAfterSearch(
    searchExpanded: Boolean,
    query: String,
    gridState: LazyGridState
) {
    var wasSearchExpanded by remember { mutableStateOf(searchExpanded) }
    var anchor by remember { mutableStateOf<ScrollAnchor?>(null) }
    LaunchedEffect(searchExpanded, query) {
        when {
            searchExpanded && !wasSearchExpanded -> {
                anchor = ScrollAnchor(
                    index = gridState.firstVisibleItemIndex,
                    offset = gridState.firstVisibleItemScrollOffset
                )
            }

            !searchExpanded && wasSearchExpanded && query.isBlank() -> {
                anchor?.let { saved ->
                    withFrameNanos { }
                    val lastIndex = gridState.layoutInfo.totalItemsCount - 1
                    if (lastIndex >= 0) {
                        gridState.scrollToItem(saved.index.coerceIn(0, lastIndex), saved.offset)
                    }
                }
                anchor = null
            }
        }
        wasSearchExpanded = searchExpanded
    }
}

private data class ScrollAnchor(
    val index: Int,
    val offset: Int
)
