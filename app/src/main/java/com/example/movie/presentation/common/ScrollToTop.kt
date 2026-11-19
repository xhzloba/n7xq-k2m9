package com.example.movie.presentation.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun ScrollToTop(request: Int, atTop: () -> Boolean, scroll: suspend () -> Unit) {
    LaunchedEffect(request) {
        if (request == 0 || atTop()) return@LaunchedEffect
        scroll()
    }
}

@Composable
internal fun LazyListState.scrollToTopOn(request: Int) {
    ScrollToTop(
        request = request,
        atTop = { firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0 },
        scroll = { animateScrollToItem(0) },
    )
}

@Composable
internal fun LazyGridState.scrollToTopOn(request: Int) {
    ScrollToTop(
        request = request,
        atTop = { firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0 },
        scroll = { animateScrollToItem(0) },
    )
}
