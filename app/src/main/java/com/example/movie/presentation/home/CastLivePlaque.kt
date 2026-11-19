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
import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.isUnreleased
import com.example.movie.domain.model.normalizedPlayerCodec
import com.example.movie.presentation.cast.ChromeCast
import com.example.movie.presentation.cast.TvCastService
import com.example.movie.presentation.common.PosterImage
import com.example.movie.presentation.detail.DetailSeed
import com.example.movie.presentation.detail.resumeWatchUrl
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
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
    CastLivePlaque(
        program = program,
        onClick = { onMovieClick(program.movie.id) },
        modifier = modifier,
    )
    Spacer(Modifier.height(24.dp))
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
                Meta(streamLine(cast.quality, settings.playerCodec))
            }
            Spacer(Modifier.width(10.dp))
            CastMark()
        }
        Spacer(Modifier.height(8.dp))
        CastBar(program)
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
private fun Meta(text: String) {
    Text(
        text = text,
        color = CinemaMuted,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun streamLine(quality: Int?, codec: String): String {
    val height = quality?.let { videoTrackLabel(it, 0) }
        ?: stringResource(R.string.player_quality_auto)
    val name = when (codec.normalizedPlayerCodec()) {
        "av1" -> "AV1"
        "vp9" -> "VP9"
        "h264" -> "H.264"
        else -> stringResource(R.string.player_quality_auto)
    }
    return "$height · $name"
}

@Composable
private fun CastBar(program: CastProgram) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StopCast(enabled = true)
        if (program.seasons.isNotEmpty()) {
            Spacer(Modifier.weight(1f))
            EpisodeSwitch(program, context)
        }
    }
}

@Composable
private fun EpisodeSwitch(program: CastProgram, context: android.content.Context) {
    val seasons = openSeasons(program.seasons)
    val seasonSpot = seasons.indexOfFirst { it.number == program.season }
    val season = seasons.getOrNull(seasonSpot)
    val episodes = season?.episodes?.filterNot { it.release.isUnreleased() }.orEmpty()
    val episodeSpot = episodes.indexOfFirst { it.number == program.episode }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Step(
            label = stringResource(R.string.franchise_season, program.season),
            canBack = seasonSpot > 0,
            canNext = seasonSpot in 0 until seasons.lastIndex,
            onBack = {
                val next = seasons.getOrNull(seasonSpot - 1) ?: return@Step
                val episode = next.episodes.firstOrNull { !it.release.isUnreleased() }?.number ?: return@Step
                jump(context, program, next.number, episode)
            },
            onNext = {
                val next = seasons.getOrNull(seasonSpot + 1) ?: return@Step
                val episode = next.episodes.firstOrNull { !it.release.isUnreleased() }?.number ?: return@Step
                jump(context, program, next.number, episode)
            },
        )
        Spacer(Modifier.width(12.dp))
        Step(
            label = stringResource(R.string.cast_episode_value, program.episode),
            canBack = episodeSpot > 0,
            canNext = episodeSpot in 0 until episodes.lastIndex,
            onBack = {
                val next = episodes.getOrNull(episodeSpot - 1)?.number ?: return@Step
                jump(context, program, program.season, next)
            },
            onNext = {
                val next = episodes.getOrNull(episodeSpot + 1)?.number ?: return@Step
                jump(context, program, program.season, next)
            },
        )
    }
}

private fun jump(context: android.content.Context, program: CastProgram, season: Int, episode: Int) {
    val watchId = program.watchId ?: return
    val next = program.copy(season = season, episode = episode)
    TvCastHub.sync(next)
    TvCastService.retarget(
        context,
        resumeWatchUrl(watchId, season, episode),
        castTitle(context, next),
        program.movie.id,
    )
}

private fun castTitle(context: android.content.Context, program: CastProgram): String {
    val title = program.movie.title
    if (program.seasons.isEmpty()) return context.getString(R.string.cast_now_movie, title)
    if (program.season <= 0 || program.episode <= 0) return context.getString(R.string.cast_now_series, title)
    return context.getString(R.string.cast_now_series_episode, title, program.season, program.episode)
}

private fun openSeasons(seasons: List<FranchiseSeason>): List<FranchiseSeason> =
    seasons.filter { season -> season.episodes.any { !it.release.isUnreleased() } }

@Composable
private fun Step(
    label: String,
    canBack: Boolean,
    canNext: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val tick = rememberHapticTick()
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepChevron(
            forward = false,
            enabled = canBack,
            onClick = {
                if (!canBack) return@StepChevron
                tick?.invoke()
                onBack()
            },
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        StepChevron(
            forward = true,
            enabled = canNext,
            onClick = {
                if (!canNext) return@StepChevron
                tick?.invoke()
                onNext()
            },
        )
    }
}

@Composable
private fun StepChevron(
    forward: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = ShellIcons.chevron,
        contentDescription = null,
        tint = Color.White.copy(alpha = if (enabled) 0.72f else 0.22f),
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = if (forward) 0f else 180f }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
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
