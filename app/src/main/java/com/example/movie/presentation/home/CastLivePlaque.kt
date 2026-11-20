package com.example.movie.presentation.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.data.cast.CastProgram
import com.example.movie.data.cast.TvCastHub
import com.example.movie.data.player.videoTrackLabel
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.normalizedPlayerCodec
import com.example.movie.presentation.cast.ChromeCast
import com.example.movie.presentation.cast.TvCastService
import com.example.movie.presentation.common.PosterImage
import com.example.movie.presentation.detail.DetailSeed
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PosterShape

private val PlaqueShape = RoundedCornerShape(18.dp)

@Composable
internal fun CastLiveSlot(
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cast by TvCastHub.state.collectAsStateWithLifecycle()
    val program = cast.player ?: return
    Column(Modifier.padding(top = 20.dp)) {
        CastLivePlaque(
            program = program,
            onClick = { onMovieClick(program.movie.id) },
            modifier = modifier,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun CastLivePlaque(
    program: CastProgram,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movie = program.movie
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val cast by TvCastHub.state.collectAsStateWithLifecycle()
    val tick = rememberHapticTick()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = rememberPressScale(pressed)
    val phase by rememberInfiniteTransition(label = "cast-snake").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 3600, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "cast-snake-phase",
    )
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = PosterGridInset),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(PlaqueShape)
                .background(Color.White.copy(alpha = 0.05f))
                .snakeBorder(phase)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        DetailSeed.put(movie)
                        onClick()
                    },
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PosterImage(
                url = movie.posterUrl,
                movieId = movie.id,
                contentDescription = movie.title,
                showLabel = false,
                modifier = Modifier
                    .size(width = 48.dp, height = 72.dp)
                    .clip(PosterShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                LiveLabel()
                Spacer(Modifier.height(6.dp))
                PlaqueTitle(
                    movie = movie,
                    logos = settings.showLogos,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                StreamFacts(
                    quality = cast.quality,
                    codec = settings.playerCodec,
                    audio = cast.audios.getOrNull(cast.audio),
                )
            }
            Spacer(Modifier.width(10.dp))
            CastMark()
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.padding(horizontal = 12.dp)) {
            StopCast(enabled = true)
        }
    }
}

private fun Modifier.snakeBorder(phase: Float): Modifier = drawWithContent {
    drawContent()
    val stroke = 1.25.dp.toPx()
    val inset = stroke / 2f
    val radius = (18.dp.toPx() - inset).coerceAtLeast(0f)
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(inset, inset, size.width - inset, size.height - inset),
                CornerRadius(radius, radius),
            ),
        )
    }
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.10f),
        style = Stroke(width = stroke),
    )
    val measure = PathMeasure().apply { setPath(path, false) }
    val length = measure.length
    if (length <= 0f) return@drawWithContent
    val span = length * 0.18f
    val head = phase * length
    val snake = Path()
    val end = head + span
    if (end <= length) {
        measure.getSegment(head, end, snake, true)
    } else {
        measure.getSegment(head, length, snake, true)
        measure.getSegment(0f, end - length, snake, true)
    }
    drawPath(
        path = snake,
        color = Color(0xFFE4E7EE),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
}

@Composable
private fun LiveLabel() {
    var width by remember { mutableIntStateOf(0) }
    val shift by rememberInfiniteTransition(label = "cast-shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1700, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "cast-shimmer-shift",
    )
    val span = width.toFloat().coerceAtLeast(1f)
    val band = span * 0.55f
    val x = -band + (span + band * 2f) * shift
    Text(
        text = stringResource(R.string.cast_plaque_live),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        onTextLayout = { width = it.size.width },
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = listOf(CinemaMuted, Color.White, CinemaMuted),
                start = Offset(x, 0f),
                end = Offset(x + band, 0f),
            ),
        ),
    )
}

@Composable
private fun PlaqueTitle(movie: Movie, logos: Boolean, modifier: Modifier = Modifier) {
    var failed by remember(movie.id, movie.logoUrl) {
        mutableStateOf(movie.logoUrl.isNullOrBlank())
    }
    if (logos && !failed) {
        AsyncImage(
            model = movie.logoUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            onError = { failed = true },
            modifier = modifier
                .wrapContentWidth(Alignment.Start)
                .height(16.dp),
        )
    } else {
        Text(
            text = movie.title,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.wrapContentWidth(Alignment.Start),
        )
    }
}

@Composable
private fun StreamFacts(quality: Int?, codec: String, audio: String?) {
    val height = quality?.let { videoTrackLabel(it, 0) }
        ?: stringResource(R.string.player_quality_auto)
    val name = when (codec.normalizedPlayerCodec()) {
        "av1" -> "AV1"
        "vp9" -> "VP9"
        "h264" -> "H.264"
        else -> stringResource(R.string.player_quality_auto)
    }
    val voice = audio?.takeIf { it.isNotBlank() } ?: "—"
    Column {
        Fact(stringResource(R.string.cast_quality_value, height))
        Fact(stringResource(R.string.download_codec_value, name))
        Fact(stringResource(R.string.cast_audio_value, voice))
    }
}

@Composable
private fun Fact(text: String) {
    Text(
        text = text,
        color = CinemaMuted,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StopCast(enabled: Boolean) {
    val context = LocalContext.current
    val tick = rememberHapticTick()
    Row(
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {
                tick?.invoke()
                if (enabled) TvCastService.stop(context)
            },
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.82f)),
            )
        }
        Text(
            text = stringResource(R.string.cast_stop),
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun CastMark() {
    val shift by rememberInfiniteTransition(label = "cast-icon").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1700, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "cast-icon-shift",
    )
    Icon(
        imageVector = ChromeCast,
        contentDescription = null,
        tint = CinemaMuted,
        modifier = Modifier
            .size(26.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val band = size.minDimension * 0.55f
                val travel = size.width + band
                val x = -band + travel * shift
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White,
                            Color.Transparent,
                        ),
                        start = Offset(x, 0f),
                        end = Offset(x + band, 0f),
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            },
    )
}
