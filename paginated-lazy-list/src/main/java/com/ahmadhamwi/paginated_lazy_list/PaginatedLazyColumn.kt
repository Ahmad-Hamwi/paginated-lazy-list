package com.ahmadhamwi.paginated_lazy_list

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun <T> PaginatedLazyColumn(
    paginationState: PaginationState<T>,
    modifier: Modifier = Modifier,
    firstPageProgressIndicator: @Composable () -> Unit = {},
    newPageProgressIndicator: @Composable () -> Unit = {},
    firstPageErrorIndicator: @Composable () -> Unit = {},
    newPageErrorIndicator: @Composable () -> Unit = {},
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    var internalState by paginationState.internalState

    LaunchedEffect(internalState) {
        if (internalState is PaginationInternalState.Loading) {
            paginationState.requestedPageNumber++
            paginationState.pageRequestListener?.invoke(paginationState.requestedPageNumber)
        }
    }

    if (internalState is PaginationInternalState.Loading && internalState.items == null) {
        firstPageProgressIndicator()
    }

    if (internalState is PaginationInternalState.Error && internalState.items == null) {
        firstPageErrorIndicator()
    }

    val pages = internalState.items

    if (pages != null) {
        LaunchedEffect(state) {
            snapshotFlow { state.layoutInfo.visibleItemsInfo.lastOrNull() }.collect { firstVisibleItemIndex ->
                if (
                    (firstVisibleItemIndex?.index ?: -1) >= pages.lastIndex
                    && (internalState as? PaginationInternalState.Loaded)?.isLastPage == false
                ) {
                    internalState = PaginationInternalState.Loading(internalState.items)
                }
            }
        }

        LazyColumn(
            modifier = modifier,
            state = state,
            contentPadding = contentPadding,
            reverseLayout = reverseLayout,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            flingBehavior = flingBehavior,
            userScrollEnabled = userScrollEnabled,
        ) {
            if (internalState.items != null) {
                content()
            }

            if (internalState is PaginationInternalState.Loading) {
                item(
                    key = LazyListKeys.NEW_PAGE_PROGRESS_INDICATOR_KEY
                ) {
                    newPageProgressIndicator()
                }
            }

            if (internalState is PaginationInternalState.Error) {
                item(
                    key = LazyListKeys.NEW_PAGE_ERROR_INDICATOR_KEY
                ) {
                    newPageErrorIndicator()
                }
            }
        }
    }

    LaunchedEffect(internalState) {
        if (internalState is PaginationInternalState.Initial) {
            internalState = PaginationInternalState.Loading(internalState.items)
        }
    }
}
