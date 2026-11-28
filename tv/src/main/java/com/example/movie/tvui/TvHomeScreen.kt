package com.example.movie.tvui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.detail.drawPlexWash
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.PosterShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SideInset = 56.dp
private val RailRest = SideInset
private val TitleHeight = 28.dp
private val LogoHeight = 58.dp
private val LogoMaxWidth = 280.dp
private val RowGap = 26.dp
private val MetaInk = Color.White.copy(alpha = 0.92f)

@Composable
internal fun TvHomeScreen(
    rails: List<TvRail>,
    activeId: String,
    onActiveId: (String) -> Unit,
    picks: MutableMap<String, Int>,
    reset: Int,
    visible: Boolean,
    posterTarget: TvPosterTarget,
    menuFocus: FocusRequester,
    menuFocused: Boolean,
    onBrowsing: (Boolean) -> Unit,
    onReachMenu: () -> Unit,
    onRetry: () -> Unit,
    backdrop: TvBackdrop,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LaunchedEffect(rails) {
        if (rails.none { it.id == activeId } && rails.isNotEmpty()) onActiveId(rails.first().id)
    }
    var seenReset by remember { mutableIntStateOf(reset) }
    LaunchedEffect(reset) {
        if (reset == seenReset) return@LaunchedEffect
        seenReset = reset
        rails.firstOrNull()?.let { onActiveId(it.id) }
        picks.clear()
    }
    val activeIndex = rails.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
    val activeRail = rails.getOrNull(activeIndex)
    val pick = activeRail?.let { rail ->
        (picks[rail.id] ?: 0).coerceIn(0, (rail.movies.size - 1).coerceAtLeast(0))
    } ?: 0
    val movie = activeRail?.movies?.getOrNull(pick)
    var held by remember { mutableStateOf<Movie?>(null) }
    if (movie != null && movie != held) held = movie
    SideEffect {
        if (visible) onBrowsing(pick > 0 && activeRail?.movies?.isNotEmpty() == true)
    }
    val shown = movie ?: held

    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val fromFrame = settings.plexBackdrop
    val motion = remember { TvWashMotion() }
    val wash = motion.current()
    SideEffect {
        if (visible) backdrop.wash = wash
    }
    val cache = remember { HashMap<String, PosterWashCache>() }
    LaunchedEffect(shown?.id, shown?.backdropUrl, shown?.posterUrl, fromFrame) {
        val item = shown ?: return@LaunchedEffect
        val url = item.washSource(fromFrame)
        val key = "${item.id}|$fromFrame|$url"
        val cached = cache[key]
        val next = if (cached != null && cached.url == url) {
            cached.wash
        } else {
            item.extractTvWash(context, fromFrame)?.also { wash ->
                cache[key] = PosterWashCache(url, wash)
            }
        } ?: return@LaunchedEffect
        motion.glide(next)
    }

    val frameUrl = shown?.backdropUrl?.takeIf { it.isNotBlank() } ?: shown?.posterUrl

    var lastRailStep by remember { mutableLongStateOf(0L) }
    val shift = remember { Animatable(activeIndex.toFloat()) }
    var shiftTarget by remember { mutableIntStateOf(activeIndex) }
    val indexJump = kotlin.math.abs(activeIndex - shiftTarget)
    SideEffect { shiftTarget = activeIndex }
    LaunchedEffect(activeIndex) {
        if (indexJump > 1) {
            shift.snapTo(activeIndex.toFloat())
            return@LaunchedEffect
        }
        val distance = kotlin.math.abs(shift.value - activeIndex)
        if (distance < 0.01f) return@LaunchedEffect
        val ms = (distance * 420f).toInt().coerceIn(220, 480)
        shift.animateTo(activeIndex.toFloat(), tween(ms, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (visible) 1f else 0f }
            .onPreviewKeyEvent { event ->
                if (!visible) return@onPreviewKeyEvent false
                val native = event.nativeKeyEvent
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                val vertical = event.key == Key.DirectionDown || event.key == Key.DirectionUp
                if (vertical && native.repeatCount > 0) {
                    val now = android.os.SystemClock.uptimeMillis()
                    if (now - lastRailStep < 180L) return@onPreviewKeyEvent true
                }
                when (event.key) {
                    Key.DirectionDown -> {
                        val next = rails.getOrNull(activeIndex + 1) ?: return@onPreviewKeyEvent true
                        lastRailStep = android.os.SystemClock.uptimeMillis()
                        onActiveId(next.id)
                        true
                    }
                    Key.DirectionUp -> {
                        val prev = rails.getOrNull(activeIndex - 1) ?: return@onPreviewKeyEvent true
                        lastRailStep = android.os.SystemClock.uptimeMillis()
                        onActiveId(prev.id)
                        true
                    }
                    else -> false
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawPlexWash(wash, 1f) },
        )
        val backdropW = maxWidth * 0.62f
        val backdropH = maxHeight * 0.72f
        AnimatedContent(
            targetState = frameUrl,
            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
            contentAlignment = Alignment.TopEnd,
            label = "backdrop",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(backdropW)
                .height(backdropH),
        ) { url ->
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.CenterEnd,
                    modifier = Modifier.fillMaxSize().cornerFade(),
                )
            }
        }
        val textW = (maxWidth * 0.50f - SideInset).coerceIn(240.dp, 560.dp)
        TvHeroMeta(
            movie = shown,
            neighbors = listOfNotNull(
                activeRail?.movies?.getOrNull(pick + 1),
                activeRail?.movies?.getOrNull(pick - 1),
            ),
            width = textW,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = SideInset, top = 28.dp),
        )
        val posterH = (maxHeight * 0.42f).coerceIn(168.dp, 280.dp)
        val rowHeight = posterH + 50.dp + RowGap
        val stageHeight = rowHeight + TitleHeight + 6.dp + posterH * 0.16f - 28.dp
        val density = LocalDensity.current
        val rowPx = with(density) { rowHeight.toPx() }
        val spanFrom = minOf(shift.value, activeIndex.toFloat())
        val spanTo = maxOf(shift.value, activeIndex.toFloat())
        val window = if (indexJump > 1) {
            (activeIndex - 1)..(activeIndex + 1)
        } else {
            (spanFrom.toInt() - 1)..(spanTo.toInt() + 1)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(stageHeight)
                .clipToBounds(),
        ) {
            rails.forEachIndexed { index, rail ->
                if (index !in window) return@forEachIndexed
                key(rail.id) {
                    TvRailRow(
                        rail = rail,
                        active = index == activeIndex,
                        pick = (picks[rail.id] ?: 0).coerceIn(0, (rail.movies.lastIndex).coerceAtLeast(0)),
                        posterHeight = posterH,
                        menuFocus = menuFocus,
                        menuFocused = menuFocused,
                        visible = visible,
                        inset = SideInset,
                        posterTarget = posterTarget,
                        onReachMenu = onReachMenu,
                        onPick = { picks[rail.id] = it },
                        onRetry = onRetry,
                        modifier = Modifier
                            .zIndex(if (index == activeIndex) 1f else 0f)
                            .offsetY { ((index - shift.value) * rowPx).roundToInt() },
                    )
                }
            }
        }
    }
}

private class PosterWashCache(
    val url: String?,
    val wash: com.example.movie.presentation.palette.PosterWash,
)

private fun Modifier.cornerFade(): Modifier = graphicsLayer {
    compositingStrategy = CompositingStrategy.Offscreen
}.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.10f to Color.Black.copy(alpha = 0.06f),
            0.22f to Color.Black.copy(alpha = 0.20f),
            0.34f to Color.Black.copy(alpha = 0.42f),
            0.46f to Color.Black.copy(alpha = 0.68f),
            0.58f to Color.Black.copy(alpha = 0.88f),
            0.68f to Color.Black,
            1f to Color.Black,
        ),
        blendMode = BlendMode.DstIn,
    )
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Black,
            0.44f to Color.Black,
            0.66f to Color.Black.copy(alpha = 0.4f),
            0.86f to Color.Black.copy(alpha = 0.08f),
            1f to Color.Transparent,
        ),
        blendMode = BlendMode.DstIn,
    )
    drawRect(
        brush = Brush.radialGradient(
            0f to Color.Transparent,
            0.16f to Color.Black.copy(alpha = 0.2f),
            0.38f to Color.Black.copy(alpha = 0.62f),
            0.58f to Color.Black,
            1f to Color.Black,
            center = Offset(size.width, 0f),
            radius = size.height * 0.78f,
        ),
        blendMode = BlendMode.DstIn,
    )
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color.Black,
            0.74f to Color.Black,
            0.86f to Color.Black.copy(alpha = 0.5f),
            0.94f to Color.Black.copy(alpha = 0.16f),
            1f to Color.Transparent,
        ),
        blendMode = BlendMode.DstIn,
    )
}

private fun Modifier.offsetY(y: () -> Int): Modifier = offset { IntOffset(0, y()) }

private data class HeroFacts(
    val runtime: Int?,
    val logo: String?,
    val overview: String,
)

@Composable
private fun TvHeroMeta(
    movie: Movie?,
    neighbors: List<Movie>,
    width: Dp,
    overviewLines: Int = 2,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val facts = remember { mutableStateMapOf<String, HeroFacts>() }
    val inflight = remember { mutableSetOf<String>() }
    LaunchedEffect(movie?.id, neighbors.firstOrNull()?.id, neighbors.getOrNull(1)?.id) {
        val focused = movie
        val queue = listOfNotNull(focused) + neighbors
        queue.distinctBy { it.id }.forEach { item ->
            if (facts.containsKey(item.id) || !inflight.add(item.id)) return@forEach
            launch {
                try {
                    val detail = container.getMovieDetail(
                        item.id,
                        background = item.id != focused?.id,
                    ).getOrNull()
                    facts[item.id] = HeroFacts(
                        runtime = detail?.runtimeMinutes,
                        logo = detail?.logoUrl?.takeIf { it.isNotBlank() } ?: item.logoUrl,
                        overview = detail?.overview?.takeIf { it.isNotBlank() } ?: item.overview,
                    )
                } finally {
                    inflight.remove(item.id)
                }
            }
        }
    }
    Box(modifier.widthIn(max = width)) {
        AnimatedContent(
            targetState = movie,
            contentKey = { it?.id },
            contentAlignment = Alignment.TopStart,
            transitionSpec = {
                (fadeIn(tween(200)) togetherWith fadeOut(tween(140)))
                    .using(SizeTransform(clip = false) { _, _ -> snap() })
            },
            label = "hero",
        ) { item ->
            if (item != null) {
                TvHeroBody(item, width, facts[item.id], overviewLines)
            } else {
                TvHeroSkeleton(width, overviewLines)
            }
        }
    }
}

@Composable
private fun TvHeroBody(movie: Movie, width: Dp, facts: HeroFacts?, overviewLines: Int) {
    val pulse = rememberInfiniteTransition(label = "hero-bone")
    val bone by pulse.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(680), RepeatMode.Reverse),
        label = "hero-bone-alpha",
    )
    val logo = facts?.logo ?: movie.logoUrl
    val overview = facts?.overview?.takeIf { it.isNotBlank() } ?: movie.overview
    val waiting = facts == null
    val runtime = facts?.runtime
    var logoFailed by remember(movie.id, logo) { mutableStateOf(logo.isNullOrBlank()) }
    Column(Modifier.width(width)) {
        when {
            logo.isNullOrBlank() && waiting -> Box(Modifier.height(LogoHeight)) {
                Bone(Modifier.width(200.dp).height(34.dp), bone)
            }
            logoFailed -> TvTitleFallback(movie.title)
            else -> TvLogo(
                title = movie.title,
                logoUrl = logo,
                width = width,
                bone = bone,
                onFailed = { logoFailed = true },
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.heightIn(min = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (movie.age != null) {
                Text(
                    text = stringResource(R.string.age_badge, movie.age),
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 0.dp),
                )
            } else if (waiting) {
                Bone(Modifier.width(28.dp).height(14.dp), bone)
            }
            if (movie.year != null) {
                Text(movie.year.toString(), color = MetaInk, fontSize = 11.sp, lineHeight = 14.sp)
            } else if (waiting) {
                Bone(Modifier.width(36.dp).height(12.dp), bone)
            }
            when {
                runtime != null -> Text(
                    runtime.asRuntime(),
                    color = MetaInk,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                )
                waiting -> Bone(Modifier.width(48.dp).height(12.dp), bone)
            }
            if (movie.genres.isNotBlank()) {
                Text(
                    text = movie.genres.twoGenres(),
                    color = MetaInk,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            } else if (waiting) {
                Bone(Modifier.width(120.dp).height(12.dp), bone)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (overview.isBlank() && waiting) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(overviewLines) { line ->
                    val fraction = when (line) {
                        0 -> 1f
                        overviewLines - 1 -> 0.66f
                        else -> 0.92f
                    }
                    Bone(Modifier.fillMaxWidth(fraction).height(10.dp), bone)
                }
            }
        } else {
            Text(
                text = overview.ifBlank { " " },
                color = Color.White.copy(alpha = if (overview.isBlank()) 0f else 0.84f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                minLines = overviewLines,
                maxLines = overviewLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TvHeroSkeleton(width: Dp, overviewLines: Int) {
    val pulse = rememberInfiniteTransition(label = "hero-empty")
    val bone by pulse.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(tween(680), RepeatMode.Reverse),
        label = "hero-empty-alpha",
    )
    Column(Modifier.width(width)) {
        Box(Modifier.height(LogoHeight), contentAlignment = Alignment.CenterStart) {
            Bone(Modifier.width(200.dp).height(34.dp), bone)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.height(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Bone(Modifier.width(28.dp).height(14.dp), bone)
            Bone(Modifier.width(36.dp).height(12.dp), bone)
            Bone(Modifier.width(48.dp).height(12.dp), bone)
            Bone(Modifier.width(120.dp).height(12.dp), bone)
        }
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(overviewLines) { line ->
                val fraction = when (line) {
                    0 -> 1f
                    overviewLines - 1 -> 0.66f
                    else -> 0.92f
                }
                Bone(Modifier.fillMaxWidth(fraction).height(10.dp), bone)
            }
        }
    }
}

@Composable
private fun Bone(modifier: Modifier, alpha: Float) {
    Box(
        modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = alpha)),
    )
}

@Composable
private fun TvTitleFallback(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun TvLogo(
    title: String,
    logoUrl: String?,
    width: Dp,
    bone: Float,
    onFailed: () -> Unit,
) {
    var ready by remember(logoUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .height(LogoHeight)
            .widthIn(max = width.coerceAtMost(LogoMaxWidth))
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (!ready) Bone(Modifier.width(200.dp).height(34.dp), bone)
        AsyncImage(
            model = logoUrl,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            onSuccess = { ready = true },
            onError = { onFailed() },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (ready) 1f else 0f },
        )
    }
}


private fun String.twoGenres(): String = split(',', '·', '•', '/')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .take(2)
    .joinToString(", ")

private fun Int.asRuntime(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "%dч %dм".format(hours, minutes)
        hours > 0 -> "%dч".format(hours)
        else -> "%dм".format(minutes)
    }
}

@Composable
private fun TvRailRow(
    rail: TvRail,
    active: Boolean,
    pick: Int,
    posterHeight: Dp,
    menuFocus: FocusRequester,
    menuFocused: Boolean,
    inset: Dp = RailRest,
    visible: Boolean,
    upFocus: FocusRequester? = null,
    posterTarget: TvPosterTarget,
    onReachMenu: () -> Unit,
    onPick: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val movies = rail.movies
    val count = if (movies.isEmpty()) 0 else movies.size
    val listState = rememberLazyListState()
    val requesters = remember(rail.id, count) { List(count) { FocusRequester() } }
    val emptyFocus = remember(rail.id) { FocusRequester() }
    val selected = pick.coerceIn(0, (count - 1).coerceAtLeast(0))
    var placed by remember(rail.id) { mutableStateOf(false) }
    if (active && visible) {
        val target = if (count == 0) emptyFocus else requesters[selected]
        posterTarget.poster = target
        posterTarget.requester = target
    }
    LaunchedEffect(active, rail.id, count, menuFocused, visible) {
        if (!active || !visible || menuFocused) return@LaunchedEffect
        val requester = if (count == 0) emptyFocus else requesters[selected]
        repeat(8) {
            val moved = runCatching { requester.requestFocus() }
            val value = moved.getOrNull()
            if (moved.isSuccess && value != false) return@LaunchedEffect
            delay(40)
        }
    }
    LaunchedEffect(active, selected, count) {
        if (!active || count == 0) return@LaunchedEffect
        if (!placed) {
            placed = true
            if (selected > 0) listState.scrollToItem(selected)
            return@LaunchedEffect
        }
        listState.animateScrollToItem(selected)
    }
    Column(modifier.fillMaxWidth()) {
        Text(
            text = stringResource(rail.titleRes),
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .padding(start = inset)
                .height(TitleHeight),
        )
        Spacer(Modifier.height(2.dp))
        if (count == 0) {
            val message = when {
                !rail.ready -> ""
                rail.failed -> stringResource(R.string.popular_load_error)
                else -> stringResource(R.string.catalog_empty)
            }
            var emptyFocused by remember(rail.id) { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .height(posterHeight + 16.dp)
                    .fillMaxWidth()
                    .focusRequester(emptyFocus)
                    .onFocusChanged { emptyFocused = it.isFocused }
                    .focusProperties {
                        canFocus = visible && (active || emptyFocused)
                        up = FocusRequester.Cancel
                        down = FocusRequester.Cancel
                        if (upFocus != null) up = upFocus
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (rail.failed) onRetry() },
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (rail.ready) {
                    Text(message, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = inset, top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        repeat(6) {
                            Box(
                                Modifier
                                    .width(posterHeight * 0.68f)
                                    .height(posterHeight)
                                    .clip(PosterShape)
                                    .background(Color.White.copy(alpha = 0.08f)),
                            )
                        }
                    }
                }
            }
        } else {
            val posterW = posterHeight * 0.68f
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = inset,
                    end = 48.dp,
                    top = 4.dp,
                    bottom = 8.dp,
                ),
                userScrollEnabled = false,
                modifier = Modifier.height(posterHeight + 16.dp),
            ) {
                itemsIndexed(movies, key = { index, item -> "${item.id}#$index" }) { index, item ->
                    val interaction = remember { MutableInteractionSource() }
                    var focused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (focused) 1.06f else 1f,
                        animationSpec = tween(160),
                        label = "poster",
                    )
                    Box(
                        modifier = Modifier
                            .width(posterW)
                            .height(posterHeight)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .focusRequester(requesters[index])
                            .onFocusChanged {
                                focused = it.isFocused
                                if (it.isFocused) onPick(index)
                            }
                            .onPreviewKeyEvent { event ->
                                if (index != 0 || !focused) return@onPreviewKeyEvent false
                                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                                    return@onPreviewKeyEvent false
                                }
                                val native = event.nativeKeyEvent
                                if (native.repeatCount > 0 || native.isLongPress) return@onPreviewKeyEvent true
                                onReachMenu()
                                true
                            }
                            .focusProperties {
                                canFocus = visible && (active || focused)
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                                if (upFocus != null) up = upFocus
                                if (index == 0) left = FocusRequester.Cancel
                                if (index == movies.lastIndex) right = FocusRequester.Cancel
                            }
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = {},
                            )
                            .border(
                                width = 2.dp,
                                color = if (focused) Color.White else Color.Transparent,
                                shape = PosterShape,
                            )
                            .clip(PosterShape)
                            .background(Color(0xFF2A2A2E)),
                        contentAlignment = Alignment.Center,
                    ) {
                        val cover = item.posterUrl?.takeIf { it.isNotBlank() }
                        var missing by remember(item.id, cover) { mutableStateOf(cover == null) }
                        if (!missing) {
                            AsyncImage(
                                model = cover,
                                contentDescription = stringResource(R.string.poster_content_description, item.title),
                                contentScale = ContentScale.Crop,
                                onError = { missing = true },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        if (missing) {
                            Icon(
                                imageVector = ShellIcons.trailers,
                                contentDescription = stringResource(R.string.poster_content_description, item.title),
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(RowGap))
    }
}
