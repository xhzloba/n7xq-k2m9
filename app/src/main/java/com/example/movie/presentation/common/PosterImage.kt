package com.example.movie.presentation.common

import android.graphics.Matrix
import android.media.MediaPlayer
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.PosterMotionManual
import com.example.movie.domain.model.normalizedPosterMotion
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import java.io.File
import java.io.IOException

/** 0 — главная и каталог. Полный список 1. Карточка фильма 2: под ней плееры гаснут. */
internal val LocalPosterLayer = compositionLocalOf { 0 }

internal const val PosterCoverLayer = 2

internal class PosterPlayTap {
    var play: (() -> Unit)? = null
}

@Composable
internal fun PosterMotionCover(layer: Int) {
    val store = LocalAppContainer.current.posterMotion
    DisposableEffect(layer) {
        val token = store.openCover(layer)
        onDispose { store.dropCover(token) }
    }
}

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
    motion: Boolean = false,
    motionPlace: String = "",
    hero: Boolean = false,
    heroActive: Boolean = false,
    playTap: PosterPlayTap? = null,
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
        if (motion && !movieId.isNullOrBlank()) {
            PosterMotion(movieId, motionPlace, hero, heroActive, playTap)
        }
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
private fun BoxScope.PosterMotion(
    movieId: String,
    place: String,
    hero: Boolean,
    heroActive: Boolean,
    playTap: PosterPlayTap?,
) {
    val store = LocalAppContainer.current.posterMotion
    val view = LocalView.current
    val layer = LocalPosterLayer.current
    val owner = LocalLifecycleOwner.current
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val manual = settings.posterMotion.normalizedPosterMotion() == PosterMotionManual
    val clips by store.clips.collectAsStateWithLifecycle()
    val slot = remember { store.openSlot() }
    DisposableEffect(slot) {
        onDispose { store.drop(slot) }
    }
    var foreground by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ ->
            val started = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            foreground = started
            store.onHostForeground(started)
        }
        owner.lifecycle.addObserver(observer)
        val started = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        foreground = started
        store.onHostForeground(started)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val live by store.live.collectAsStateWithLifecycle()
    val playing = foreground && slot in live
    var file by remember(movieId) { mutableStateOf<File?>(null) }
    var frame by remember(movieId) { mutableStateOf(false) }
    var fraction by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                val width = view.width.toFloat().coerceAtLeast(1f)
                val height = view.height.toFloat().coerceAtLeast(1f)
                val shownW = (minOf(bounds.right, width) - maxOf(bounds.left, 0f)).coerceAtLeast(0f)
                val shownH = (minOf(bounds.bottom, height) - maxOf(bounds.top, 0f)).coerceAtLeast(0f)
                val area = bounds.width * bounds.height
                fraction = if (area <= 1f) 0f else (shownW * shownH / area).coerceIn(0f, 1f)
            },
    )
    LaunchedEffect(movieId, place, layer, fraction, hero, heroActive, manual) {
        store.report(slot, movieId, place, layer, fraction, hero, heroActive, manual)
    }
    val tick = rememberHapticTick()
    val showPlay = manual && movieId in clips && !playing
    SideEffect {
        playTap?.play = if (showPlay) {
            {
                tick?.invoke()
                store.ask(slot)
            }
        } else {
            null
        }
    }
    DisposableEffect(playTap) {
        onDispose { playTap?.play = null }
    }
    if (showPlay) {
        PosterPlayMark(Modifier.align(Alignment.TopStart))
    }
    LaunchedEffect(movieId, playing) {
        if (!playing) {
            frame = false
            return@LaunchedEffect
        }
        val ready = store.fileFor(movieId)
        if (ready == null) {
            store.complete(slot)
            return@LaunchedEffect
        }
        file = ready
    }
    val clip = file
    if (!playing || clip == null) return
    key(clip.path) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (frame) 1f else 0f },
            factory = { context ->
                TextureView(context).apply {
                    isOpaque = false
                    val player = MediaPlayer()
                    tag = player
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: android.graphics.SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            player.setSurface(android.view.Surface(surface))
                            player.isLooping = false
                            player.setVolume(0f, 0f)
                            player.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                cover(videoWidth, videoHeight)
                            }
                            player.setOnPreparedListener { ready ->
                                store.seen(slot)
                                cover(ready.videoWidth, ready.videoHeight)
                                ready.setVolume(0f, 0f)
                                ready.start()
                            }
                            player.setOnCompletionListener { store.complete(slot) }
                            player.setOnErrorListener { _, _, _ ->
                                store.complete(slot)
                                true
                            }
                            val opened = runCatching {
                                player.setDataSource(clip.path)
                                player.prepareAsync()
                            }
                            if (opened.isFailure) player.release()
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: android.graphics.SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            cover(player.videoWidth, player.videoHeight)
                        }

                        override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                            runCatching { player.setSurface(null) }
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
                            if (!frame) frame = true
                        }
                    }
                }
            },
            onRelease = { view ->
                val player = view.tag as? MediaPlayer ?: return@AndroidView
                view.tag = null
                view.surfaceTextureListener = null
                runCatching { player.reset() }
                player.release()
            },
        )
    }
}

@Composable
private fun PosterPlayMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = stringResource(R.string.poster_motion_play),
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun TextureView.cover(videoWidth: Int, videoHeight: Int) {
    if (videoWidth <= 0 || videoHeight <= 0) return
    post {
        if (width <= 0 || height <= 0) return@post
        val viewRatio = width.toFloat() / height
        val videoRatio = videoWidth.toFloat() / videoHeight
        val sx: Float
        val sy: Float
        if (videoRatio > viewRatio) {
            sx = videoRatio / viewRatio
            sy = 1f
        } else {
            sx = 1f
            sy = viewRatio / videoRatio
        }
        setTransform(Matrix().apply { setScale(sx, sy, width / 2f, height / 2f) })
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
