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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.common.ImdbScore
import com.example.movie.presentation.common.KpScore
import com.example.movie.presentation.detail.drawPlexWash
import com.example.movie.ui.theme.PosterShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SideInset = 56.dp
private val RailRest = SideInset
private val TitleHeight = 28.dp
private val RowGap = 26.dp
private val Peek = TitleHeight
private val MetaInk = Color.White.copy(alpha = 0.92f)

@Composable
internal fun TvHomeScreen(
    rails: List<TvRail>,
    posterTarget: TvPosterTarget,
    menuFocus: FocusRequester,
    menuFocused: Boolean,
    onBrowsing: (Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var activeId by rememberSaveable { mutableStateOf(rails.firstOrNull()?.id ?: "popular") }
    LaunchedEffect(rails) {
        if (rails.none { it.id == activeId } && rails.isNotEmpty()) activeId = rails.first().id
    }
    val activeIndex = rails.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
    val activeRail = rails.getOrNull(activeIndex)
    val picks = remember { mutableStateMapOf<String, Int>() }
    val pick = activeRail?.let { rail ->
        (picks[rail.id] ?: 0).coerceIn(0, (rail.movies.size - 1).coerceAtLeast(0))
    } ?: 0
    val movie = activeRail?.movies?.getOrNull(pick)
    SideEffect { onBrowsing(pick > 0 && activeRail?.movies?.isNotEmpty() == true) }
    val shown = movie

    val motion = remember { TvWashMotion() }
    val wash = motion.current()
    val cache = remember { HashMap<String, PosterWashCache>() }
    LaunchedEffect(shown?.id, shown?.backdropUrl, shown?.posterUrl) {
        val item = shown ?: return@LaunchedEffect
        val cached = cache[item.id]
        val next = if (cached != null && cached.url == item.washUrl()) {
            cached.wash
        } else {
            item.extractBackdropWash(context)?.also { wash ->
                cache[item.id] = PosterWashCache(item.washUrl(), wash)
            }
        } ?: return@LaunchedEffect
        motion.glide(next)
    }

    val frameUrl = shown?.backdropUrl?.takeIf { it.isNotBlank() } ?: shown?.posterUrl

    val shift = remember { Animatable(activeIndex.toFloat()) }
    LaunchedEffect(activeIndex) {
        shift.animateTo(activeIndex.toFloat(), tween(420, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.nativeKeyEvent.repeatCount > 0) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionDown -> {
                        val next = rails.getOrNull(activeIndex + 1) ?: return@onPreviewKeyEvent true
                        activeId = next.id
                        true
                    }
                    Key.DirectionUp -> {
                        val prev = rails.getOrNull(activeIndex - 1)
                        if (prev != null) activeId = prev.id
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
        val textW = (maxWidth * 0.50f - SideInset).coerceIn(280.dp, 560.dp)
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
        val posterH = (maxHeight * 0.36f).coerceIn(148.dp, 240.dp)
        val rowHeight = posterH + 50.dp + RowGap
        val stageHeight = rowHeight + Peek
        val density = LocalDensity.current
        val rowPx = with(density) { rowHeight.toPx() }
        val window = (activeIndex - 1)..(activeIndex + 1)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 28.dp)
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
                        posterTarget = posterTarget,
                        onPick = { picks[rail.id] = it },
                        onRetry = onRetry,
                        modifier = Modifier
                            .zIndex(if (index == activeIndex) 1f else 0f)
                            .offsetY { ((index - shift.value) * rowPx).roundToInt() },
                    )
                }
            }
        }
        Spacer(Modifier.height(RowGap))
    }
}

private class PosterWashCache(
    val url: String?,
    val wash: com.example.movie.presentation.palette.PosterWash,
)

private fun Movie.washUrl(): String? = backdropUrl?.takeIf { it.isNotBlank() } ?: posterUrl

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
            if (item != null) TvHeroBody(item, width, facts[item.id])
        }
    }
}

@Composable
private fun TvHeroBody(movie: Movie, width: Dp, facts: HeroFacts?) {
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
    Column(Modifier.width(width)) {
        Box(Modifier.height(46.dp)) {
            if (logo.isNullOrBlank() && waiting) {
                Bone(Modifier.width(168.dp).height(28.dp), bone)
            } else {
                TvLogo(title = movie.title, logoUrl = logo, width = width)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.heightIn(min = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            movie.age?.let { age ->
                Text(
                    text = stringResource(R.string.age_badge, age),
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.72f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 0.dp),
                )
            }
            movie.year?.let { year ->
                Text(year.toString(), color = MetaInk, fontSize = 11.sp, lineHeight = 14.sp)
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
                    text = movie.genres,
                    color = MetaInk,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.heightIn(min = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            movie.ratingImdb?.let { score ->
                ImdbScore(
                    score = score,
                    color = Color.White,
                    iconHeight = 10.dp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            movie.ratingKp?.let { score ->
                KpScore(
                    score = score,
                    color = Color.White,
                    iconSize = 10.dp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            if (movie.ratingImdb == null && movie.ratingKp == null && waiting) {
                Bone(Modifier.width(56.dp).height(12.dp), bone)
                Bone(Modifier.width(56.dp).height(12.dp), bone)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (overview.isBlank() && waiting) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Bone(Modifier.fillMaxWidth().height(10.dp), bone)
                Bone(Modifier.fillMaxWidth(0.92f).height(10.dp), bone)
                Bone(Modifier.fillMaxWidth(0.66f).height(10.dp), bone)
            }
        } else {
            Text(
                text = overview.ifBlank { " " },
                color = Color.White.copy(alpha = if (overview.isBlank()) 0f else 0.84f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                minLines = 3,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
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
private fun TvLogo(title: String, logoUrl: String?, width: Dp) {
    var failed by remember(logoUrl) { mutableStateOf(logoUrl.isNullOrBlank()) }
    if (!failed) {
        AsyncImage(
            model = logoUrl,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            onError = { failed = true },
            modifier = Modifier
                .height(46.dp)
                .widthIn(max = width.coerceAtMost(240.dp))
                .fillMaxWidth(),
        )
    } else {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

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
    posterTarget: TvPosterTarget,
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
    if (active) {
        posterTarget.requester = if (count == 0) emptyFocus else requesters[selected]
    }
    LaunchedEffect(active, rail.id, count, menuFocused) {
        if (!active || menuFocused) return@LaunchedEffect
        val requester = if (count == 0) emptyFocus else requesters[selected]
        repeat(8) {
            val moved = runCatching { requester.requestFocus() }
            val value = moved.getOrNull()
            if (moved.isSuccess && value != false) return@LaunchedEffect
            delay(40)
        }
    }
    LaunchedEffect(active, selected, count) {
        if (!active || count == 0 || selected == 0) return@LaunchedEffect
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
                .padding(start = RailRest)
                .height(TitleHeight),
        )
        Spacer(Modifier.height(6.dp))
        if (count == 0) {
            val message = when {
                !rail.ready -> ""
                rail.failed -> stringResource(R.string.popular_load_error)
                else -> stringResource(R.string.catalog_empty)
            }
            Box(
                modifier = Modifier
                    .height(posterHeight)
                    .fillMaxWidth()
                    .focusRequester(emptyFocus)
                    .focusProperties { canFocus = active }
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
                        modifier = Modifier.padding(start = RailRest),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = RailRest,
                    end = 48.dp,
                    top = 8.dp,
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
                            .focusProperties {
                                canFocus = active
                                if (index == 0) left = menuFocus
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
                    ) {
                        AsyncImage(
                            model = item.posterUrl ?: item.backdropUrl,
                            contentDescription = stringResource(R.string.poster_content_description, item.title),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(RowGap))
    }
}
