package com.example.movie.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import java.io.IOException

@Composable
internal fun PosterImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    iconSize: Dp = 28.dp,
    onSuccess: (() -> Unit)? = null,
    movieId: String? = null,
    fallbackUrl: String? = null,
) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val shown = moviePoster(movieId, url, fallbackUrl)
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val model = posterForCell(shown, width, settings.posterQuality)
        PosterBitmap(
            url = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            iconSize = iconSize,
            onSuccess = onSuccess,
        )
    }
}

@Composable
internal fun moviePoster(movieId: String?, api: String?, fallback: String? = null): String? {
    val poster = api?.takeIf { it.isNotBlank() }
    val spare = fallback?.takeIf { it.isNotBlank() }
    if (movieId.isNullOrBlank()) return poster ?: spare
    val picks by LocalAppContainer.current.backdropSets.sets.collectAsStateWithLifecycle()
    return picks[movieId]?.chosenPoster(poster) ?: poster ?: spare
}

@Composable
private fun PosterBitmap(
    url: String?,
    contentDescription: String?,
    contentScale: ContentScale,
    iconSize: Dp,
    onSuccess: (() -> Unit)?,
) {
    // Один сетевой промах нельзя запоминать на весь заход: ячейка так и
    // остаётся пустой, хотя тот же url на следующем заходе открывается.
    // Два повтора на IO, потом плейсхолдер. Новый показ ячейки начинает сначала.
    var attempt by remember(url) { mutableIntStateOf(0) }
    var loaded by remember(url) { mutableStateOf(false) }
    val missing = url.isNullOrBlank()
    val giveUp = missing || attempt > 2
    LaunchedEffect(giveUp) {
        if (giveUp) onSuccess?.invoke()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (!loaded) {
            PosterHold(contentDescription, iconSize)
        }
        if (!giveUp) {
            key(attempt) {
                AsyncImage(
                    model = url,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    onError = { state ->
                        val error = state.result.throwable
                        attempt = if (error is IOException && attempt < 2) attempt + 1 else 3
                    },
                    onSuccess = {
                        loaded = true
                        onSuccess?.invoke()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PosterHold(
    contentDescription: String?,
    iconSize: Dp,
) {
    Icon(
        imageVector = ShellIcons.trailers,
        contentDescription = contentDescription,
        tint = Color.White.copy(alpha = 0.45f),
        modifier = Modifier.size(iconSize),
    )
}
