package com.example.movie.presentation.common

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos

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
        scroll = { glideToTop() },
    )
}

@Composable
internal fun ScrollState.scrollToTopOn(request: Int) {
    ScrollToTop(
        request = request,
        atTop = { value == 0 },
        scroll = { glideToStart() },
    )
}

@Composable
internal fun LazyGridState.scrollToTopOn(request: Int) {
    ScrollToTop(
        request = request,
        atTop = { firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0 },
        scroll = { glideToTop() },
    )
}

internal suspend fun ScrollState.glideToStart() {
    glide(
        atTop = { value == 0 },
        pixelsAbove = { value.toFloat() },
    )
}

private suspend fun LazyListState.glideToTop() {
    glide(
        atTop = { firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0 },
        pixelsAbove = {
            val items = layoutInfo.visibleItemsInfo
            val average = if (items.isEmpty()) {
                0f
            } else {
                items.sumOf { it.size.toLong() }.toFloat() / items.size
            }
            firstVisibleItemIndex * average + firstVisibleItemScrollOffset
        },
    )
}

private suspend fun LazyGridState.glideToTop() {
    glide(
        atTop = { firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0 },
        pixelsAbove = {
            val items = layoutInfo.visibleItemsInfo
            val average = if (items.isEmpty()) {
                0f
            } else {
                items.sumOf { it.size.height.toLong() }.toFloat() / items.size
            }
            firstVisibleItemIndex * average + firstVisibleItemScrollOffset
        },
    )
}

private suspend fun ScrollableState.glide(atTop: () -> Boolean, pixelsAbove: () -> Float) {
    if (atTop()) return
    val estimate = pixelsAbove().coerceAtLeast(1f)
    runGlide(distance = estimate * 1.25f, durationNs = glideDuration(estimate), atTop = atTop)
    if (!atTop()) {
        val rest = pixelsAbove().coerceAtLeast(1f)
        runGlide(distance = rest, durationNs = 140_000_000L, atTop = atTop)
    }
}

private fun glideDuration(pixels: Float): Long = when {
    pixels < 800f -> 220_000_000L
    pixels < 2400f -> 280_000_000L
    else -> 360_000_000L
}

private suspend fun ScrollableState.runGlide(
    distance: Float,
    durationNs: Long,
    atTop: () -> Boolean,
) {
    scroll {
        var start = 0L
        var traveled = 0f
        while (!atTop()) {
            val now = withFrameNanos { it }
            if (start == 0L) start = now
            val t = ((now - start).toFloat() / durationNs).coerceIn(0f, 1f)
            val target = distance * easeInOut(t)
            val step = target - traveled
            traveled = target
            if (step >= 1f && scrollBy(-step) == 0f) break
            if (t >= 1f) break
        }
    }
}

private fun easeInOut(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return if (x < 0.5f) {
        4f * x * x * x
    } else {
        val u = -2f * x + 2f
        1f - (u * u * u) / 2f
    }
}
