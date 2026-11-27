package com.example.movie.tvui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.palette.PosterWash
import com.example.movie.presentation.palette.plexWash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class TvWashMotion {
    val topLeft = Animatable(Color(0xFF101014))
    val topRight = Animatable(Color(0xFF101014))
    val bottomRight = Animatable(Color(0xFF101014))
    val bottomLeft = Animatable(Color(0xFF101014))

    @Composable
    fun current(): PosterWash = PosterWash(
        topLeft = topLeft.value,
        topRight = topRight.value,
        bottomRight = bottomRight.value,
        bottomLeft = bottomLeft.value,
    )

    suspend fun glide(wash: PosterWash) {
        val spec = tween<Color>(680, easing = FastOutSlowInEasing)
        coroutineScope {
            launch { topLeft.animateTo(wash.topLeft, spec) }
            launch { topRight.animateTo(wash.topRight, spec) }
            launch { bottomRight.animateTo(wash.bottomRight, spec) }
            launch { bottomLeft.animateTo(wash.bottomLeft, spec) }
        }
    }
}

/** UltraPlex с backdrop, тот же `plexWash`, что красит мобильную главную и деталку. */
internal suspend fun Movie.extractBackdropWash(context: Context): PosterWash? {
    val url = backdropUrl?.takeIf { it.isNotBlank() } ?: posterUrl?.takeIf { it.isNotBlank() } ?: return null
    val result = context.imageLoader.execute(
        ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(480, 270)
            .scale(Scale.FIT)
            .build(),
    )
    val source = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap ?: return null
    val bitmap = if (source.config == Bitmap.Config.HARDWARE) {
        source.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    } else {
        source
    }
    val wash = withContext(Dispatchers.Default) { bitmap.plexWash() }
    if (bitmap !== source) bitmap.recycle()
    return wash
}
