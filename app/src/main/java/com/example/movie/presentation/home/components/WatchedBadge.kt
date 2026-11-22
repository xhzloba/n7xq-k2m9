package com.example.movie.presentation.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.PlayBlue

internal val WatchedBadgeHang = 6.dp

@Composable
internal fun rememberWatchedIds(): Set<String> {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val ids by LocalAppContainer.current.watched.ids.collectAsStateWithLifecycle()
    return if (settings.watchedBadge) ids else emptySet()
}

@Composable
internal fun rememberFavoriteIds(): Set<String> {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val ids by LocalAppContainer.current.favorites.ids.collectAsStateWithLifecycle()
    return if (settings.watchedBadge) ids else emptySet()
}

@Composable
internal fun WatchedBadge(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(22.dp)
            .offset(x = (-4).dp, y = 6.dp),
    ) {
        drawCircle(color = PlayBlue, radius = size.minDimension / 2f)
        val mark = Path().apply {
            moveTo(size.width * 8.3f / 24f, size.height * 12.2f / 24f)
            lineTo(size.width * 10.9f / 24f, size.height * 14.8f / 24f)
            lineTo(size.width * 15.8f / 24f, size.height * 9.4f / 24f)
        }
        drawPath(
            mark,
            Color.White,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
internal fun FavoriteBadge(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .size(22.dp)
            .offset(x = (-4).dp, y = 6.dp),
    ) {
        drawCircle(color = Color.White, radius = size.minDimension / 2f)
        drawPath(favoriteHeart(), Color.Black)
    }
}

private fun DrawScope.favoriteHeart(): Path {
    val pad = 3.6.dp.toPx()
    val span = size.minDimension - pad * 2
    fun x(value: Float) = pad + value / 24f * span
    fun y(value: Float) = pad + value / 24f * span
    return Path().apply {
        moveTo(x(12f), y(21.35f))
        lineTo(x(10.55f), y(20.03f))
        cubicTo(x(5.4f), y(15.36f), x(2f), y(12.28f), x(2f), y(8.5f))
        cubicTo(x(2f), y(5.42f), x(4.42f), y(3f), x(7.5f), y(3f))
        cubicTo(x(9.24f), y(3f), x(10.91f), y(3.81f), x(12f), y(5.09f))
        cubicTo(x(13.09f), y(3.81f), x(14.76f), y(3f), x(16.5f), y(3f))
        cubicTo(x(19.58f), y(3f), x(22f), y(5.42f), x(22f), y(8.5f))
        cubicTo(x(22f), y(12.28f), x(18.6f), y(15.36f), x(13.45f), y(20.04f))
        close()
    }
}
