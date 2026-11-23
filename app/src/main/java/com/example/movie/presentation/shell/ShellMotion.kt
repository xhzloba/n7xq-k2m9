package com.example.movie.presentation.shell

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

internal fun Modifier.slideBy(fraction: () -> Float, widthPx: Int): Modifier =
    offset { IntOffset((fraction() * widthPx).roundToInt(), 0) }

internal fun Modifier.holdOffscreen(): Modifier =
    layout { measurable, constraints ->
        if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
            val full = constraints.copy(
                minWidth = constraints.maxWidth,
                minHeight = constraints.maxHeight,
            )
            measurable.measure(full)
        }
        layout(0, 0) {}
    }.clearAndSetSemantics {}
