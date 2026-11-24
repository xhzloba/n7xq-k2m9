@file:OptIn(ExperimentalFoundationApi::class)

package com.example.movie.presentation.home.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.detail.DetailSeed
import com.example.movie.presentation.detail.drawPlexWash
import com.example.movie.presentation.common.PosterImage
import com.example.movie.presentation.common.PosterPlayTap
import com.example.movie.presentation.common.appChrome
import com.example.movie.presentation.home.ShelfFillSheet
import com.example.movie.presentation.home.ShelfFillUltra
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.shelfYearWhite
import com.example.movie.presentation.palette.PosterWash
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.pressToOpen
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.presentation.haptic.HapticOnAppend
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.domain.model.HapticSlot
import com.example.movie.presentation.haptic.rememberHaptic
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PosterShape
import java.util.LinkedHashMap
import kotlinx.coroutines.flow.distinctUntilChanged

/** Цвета первого постера полки. Живёт дольше LazyColumn, чтобы не считать постер заново. */
private val shelfWashes = object : LinkedHashMap<String, PosterWash>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PosterWash>?): Boolean = size > 32
}

private fun Movie.hasArt(): Boolean =
    !posterUrl.isNullOrBlank() || !backdropUrl.isNullOrBlank()

private val PosterWidth = 112.dp
private val RailStart = PosterGridInset
internal val PosterGap = 8.dp
private val SideRetouch = 56.dp

@Composable
internal fun RailTitle(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    shimmer: Boolean = false,
    onClose: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onAnchor: ((Rect) -> Unit)? = null,
) {
    val tick = rememberHapticTick()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val white = settings.whiteTitles
    val color = if (white) Color.White else Color(0xFFB4B4B8)
    val titleSize = if (white) 18.sp else CategoryTabFontSize
    val density = LocalDensity.current
    val iconSize = with(density) { titleSize.toDp() }
    var titleWidth by remember { mutableIntStateOf(0) }
    val iconPx = with(density) { iconSize.toPx() }
    val pullPx = with(density) { (-(iconSize * 9f / 24f) + 8.dp).toPx() }
    val span = titleWidth + pullPx + iconPx
    val shift = if (shimmer) titleShimmerShift() else 0f
    val iconOrigin = titleWidth + pullPx
    Row(
        modifier = modifier
            .then(if (onClose != null) Modifier.fillMaxWidth() else Modifier)
            .padding(horizontal = PosterGridInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (onAnchor == null) {
                        Modifier
                    } else {
                        Modifier.onGloballyPositioned { onAnchor(it.boundsInRoot()) }
                    },
                )
                .then(
                if (onClick == null && onLongClick == null) {
                    Modifier
                } else {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (onClick != null) {
                                tick?.invoke()
                                onClick()
                            }
                        },
                        onLongClick = onLongClick?.let { longClick ->
                            {
                                tick?.invoke()
                                longClick()
                            }
                        },
                    )
                },
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            color = if (shimmer) Color.Unspecified else color,
            fontWeight = if (white) FontWeight.Bold else FontWeight.Normal,
            fontSize = titleSize,
            lineHeight = titleSize,
            letterSpacing = if (white) TextUnit.Unspecified else CategoryTabLetterSpacing,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = if (shimmer) {
                { titleWidth = it.size.width }
            } else {
                null
            },
            style = if (shimmer) {
                TextStyle(
                    brush = shimmerBrush(color, span, shift, 0f),
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                )
            } else {
                TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
            },
        )
        if (onClick != null) {
            val chevron = if (shimmer) shimmerBrush(color, span, shift, iconOrigin) else null
            Icon(
                imageVector = if (white) ShellIcons.chevronBold else ShellIcons.chevron,
                contentDescription = null,
                tint = if (shimmer) Color.White else color,
                modifier = Modifier
                    .offset(x = -(iconSize * 9f / 24f) + 8.dp)
                    .size(iconSize)
                    .then(
                        if (chevron == null) {
                            Modifier
                        } else {
                            Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = chevron, blendMode = BlendMode.SrcIn)
                                }
                        },
                    ),
            )
        }
        }
        if (onClose != null) {
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onClose()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ShellIcons.close,
                    contentDescription = stringResource(R.string.player_close),
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun titleShimmerShift(): Float {
    val shift by rememberInfiniteTransition(label = "rail-title-shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1400, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "rail-title-shimmer-shift",
    )
    return shift
}

private fun shimmerBrush(color: Color, span: Float, shift: Float, origin: Float): Brush {
    val width = span.coerceAtLeast(1f)
    val band = width * 0.55f
    val head = -band + (width + band * 2f) * shift
    val x = head - origin
    return Brush.linearGradient(
        colors = listOf(color.copy(alpha = 0.45f), Color.White, color.copy(alpha = 0.45f)),
        start = Offset(x, 0f),
        end = Offset(x + band, 0f),
    )
}

@Composable
internal fun RecommendationRail(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    leadingIcon: ImageVector? = null,
    onOpen: (() -> Unit)? = null,
    onSettled: () -> Unit = {},
    stickToStart: Boolean = false,
    progressOf: (Movie) -> Float? = { null },
    captionOf: (Movie) -> String? = { null },
    seeAllGlass: Boolean = false,
    overlay: @Composable (Movie) -> Unit = {},
    titleShimmer: Boolean = false,
    onClose: (() -> Unit)? = null,
    motion: Boolean = false,
    motionPlace: String = "",
    fillKey: String? = null,
    onTitleAnchor: ((Rect) -> Unit)? = null,
    onFillOpen: (() -> Unit)? = null,
) {
    val settingsRepo = LocalUserSettings.current
    val settings by settingsRepo.settings.collectAsStateWithLifecycle()
    val fill = fillKey?.let { settings.shelfFills[it] } ?: 0
    val ultra = fill == ShelfFillUltra
    val flat = fill != 0 && !ultra
    val yearWhite = ultra || (flat && fill.shelfYearWhite())
    var picking by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val leadId = if (ultra || picking) movies.firstOrNull { it.hasArt() }?.id else null
    var ultraWash by remember { mutableStateOf(leadId?.let { shelfWashes[it] }) }
    LaunchedEffect(leadId) {
        val id = leadId ?: return@LaunchedEffect
        val cached = shelfWashes[id]
        if (cached != null) {
            ultraWash = cached
            return@LaunchedEffect
        }
        ultraWash = null
        val movie = movies.firstOrNull { it.id == id } ?: return@LaunchedEffect
        val wash = movie.extractPosterWash(context) ?: return@LaunchedEffect
        shelfWashes[id] = wash
        ultraWash = wash
    }
    val wash = ultraWash
    HapticOnAppend(movies.map { it.id })
    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer()
            .then(
                when {
                    ultra && wash != null -> Modifier.drawBehind { drawPlexWash(wash, 1f) }
                    flat -> Modifier.background(Color(fill))
                    else -> Modifier
                },
            )
            .padding(top = if (fill != 0) 12.dp else 0.dp, bottom = if (fill != 0) 12.dp else 0.dp),
    ) {
        if (title != null) {
            RailTitle(
                title = title,
                onClick = onOpen,
                leadingIcon = leadingIcon,
                shimmer = titleShimmer,
                onClose = onClose,
                onLongClick = if (fillKey == null) null else {
                    {
                        onFillOpen?.invoke()
                        picking = true
                    }
                },
                onAnchor = onTitleAnchor,
            )
            Spacer(Modifier.height(10.dp))
        }
        if (movies.isEmpty()) {
            RecommendationRailSkeleton()
        } else {
            RecommendationPager(
                movies = movies,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
                onOpen = onOpen,
                onSettled = onSettled,
                stickToStart = stickToStart,
                progressOf = progressOf,
                captionOf = captionOf,
                seeAllGlass = seeAllGlass,
                overlay = overlay,
                motion = motion,
                motionPlace = motionPlace,
                yearWhite = yearWhite,
            )
        }
    }
    if (picking && title != null && fillKey != null) {
        ShelfFillSheet(
            title = title,
            color = fill,
            wash = wash,
            onColor = { settingsRepo.setShelfFill(fillKey, it) },
            onDismiss = { picking = false },
        )
    }
}

@Composable
internal fun RecommendationRailSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val posterWidth = posterSize(PosterWidth)
            val posterHeight = posterWidth * posterAspect()
            val rowHeight = posterHeight + WatchedBadgeHang + posterCaptionExtra()
            val count = ((maxWidth - RailStart) / (posterWidth + PosterGap)).toInt().coerceIn(2, 4)
            LazyRow(
                contentPadding = PaddingValues(start = RailStart, end = RailStart),
                horizontalArrangement = Arrangement.spacedBy(PosterGap),
                userScrollEnabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
            ) {
                items(count) {
                    Column(Modifier.width(posterWidth)) {
                        Box(
                            modifier = Modifier
                                .width(posterWidth)
                                .height(posterHeight)
                                .clip(PosterShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                        )
                        Spacer(Modifier.height(WatchedBadgeHang))
                        PosterCaptionSkeleton()
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationPager(
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onOpen: (() -> Unit)? = null,
    onSettled: () -> Unit,
    stickToStart: Boolean = false,
    progressOf: (Movie) -> Float? = { null },
    captionOf: (Movie) -> String? = { null },
    seeAllGlass: Boolean = false,
    overlay: @Composable (Movie) -> Unit = {},
    motion: Boolean = false,
    motionPlace: String = "",
    yearWhite: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val head = movies.firstOrNull()?.id
    // Глушим только программный прыжок в начало, когда фильм встал первым.
    // Палец и инерция щелкают как раньше: флаг читается в момент смены индекса.
    val mute = remember { booleanArrayOf(false) }
    val muteGen = remember { intArrayOf(0) }
    val seenHead = remember { arrayOf<String?>(null) }
    SideEffect {
        if (stickToStart && head != null && seenHead[0] != head) {
            seenHead[0] = head
            muteGen[0]++
            mute[0] = true
        }
    }
    LaunchedEffect(head) {
        if (!stickToStart || head == null) return@LaunchedEffect
        val ticket = muteGen[0]
        try {
            listState.scrollToItem(0)
            withFrameNanos { }
        } finally {
            if (muteGen[0] == ticket) mute[0] = false
        }
    }
    val tick = rememberHapticTick()
    val poster = rememberHaptic(HapticSlot.Poster)
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    // UltraBlur на главной красит фон — боковая ретушь полок тогда выкл.
    val sideRetouch = settings.sliderShade && !settings.heroPosterColors
    val ink = appChrome()
    val scrolling = listState.isScrollInProgress
    // +1: контент уходит влево (листаем влево) → ретушь слева
    // -1: контент уходит вправо (листаем вправо) → ретушь справа
    var scrollDir by remember { mutableIntStateOf(0) }
    LaunchedEffect(listState, sideRetouch) {
        if (!sideRetouch) {
            scrollDir = 0
            return@LaunchedEffect
        }
        var lastIndex = listState.firstVisibleItemIndex
        var lastOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                val delta = when {
                    index > lastIndex -> 1
                    index < lastIndex -> -1
                    offset > lastOffset -> 1
                    offset < lastOffset -> -1
                    else -> 0
                }
                if (delta != 0) scrollDir = delta
                lastIndex = index
                lastOffset = offset
            }
    }
    val leftTarget = if (sideRetouch && scrolling && scrollDir > 0) 1f else 0f
    val rightTarget = if (sideRetouch && scrolling && scrollDir < 0) 1f else 0f
    val leftStrength by animateFloatAsState(
        targetValue = leftTarget,
        animationSpec = tween(
            durationMillis = if (leftTarget > 0f) 520 else 640,
            easing = FastOutSlowInEasing,
        ),
        label = "railRetouchL",
    )
    val rightStrength by animateFloatAsState(
        targetValue = rightTarget,
        animationSpec = tween(
            durationMillis = if (rightTarget > 0f) 520 else 640,
            easing = FastOutSlowInEasing,
        ),
        label = "railRetouchR",
    )
    SettleEffect(listState, { listState.isScrollInProgress }, onSettled)
    HapticOnChange(listState, armed = { !mute[0] }) { listState.firstVisibleItemIndex }
    val decay = rememberSplineBasedDecay<Float>()
    val snapping = remember(listState) { SnapLayoutInfoProvider(listState, SnapPosition.Start) }
    val flingBehavior = remember(snapping, decay) {
        snapFlingBehavior(
            snapLayoutInfoProvider = snapping,
            decayAnimationSpec = decay,
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    val posterWidth = posterSize(PosterWidth)
    val posterHeight = posterWidth * posterAspect()
    val watched = rememberWatchedIds()
    val favorites = rememberFavoriteIds()
    val rowHeight = posterHeight + WatchedBadgeHang + posterCaptionExtra()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Плашка «Все» занимает хвост после последнего постера, когда доскроллили до конца.
        val trail = onOpen != null
        val endPad = RailStart
        val trailWidth = if (trail) {
            (maxWidth - RailStart - posterWidth - PosterGap - endPad).coerceAtLeast(72.dp)
        } else {
            0.dp
        }
        val end = if (trail) {
            endPad
        } else {
            (maxWidth - posterWidth - RailStart).coerceAtLeast(RailStart)
        }
        CompositionLocalProvider(LocalOverscrollConfiguration provides OverscrollConfiguration()) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(start = RailStart, end = end),
            horizontalArrangement = Arrangement.spacedBy(PosterGap),
            flingBehavior = flingBehavior,
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight)
                .then(
                    if (leftStrength > 0.01f || rightStrength > 0.01f) {
                        Modifier.railSideRetouch(
                            ink = ink,
                            width = SideRetouch,
                            left = leftStrength,
                            right = rightStrength,
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            itemsIndexed(movies, key = { _, movie -> movie.id }) { _, movie ->
                var holdProgress by remember(movie.id) { mutableFloatStateOf(0f) }
                var pressed by remember(movie.id) { mutableStateOf(false) }
                val playTap = remember(movie.id) { PosterPlayTap() }
                val pressScale = rememberPressScale(pressed)
                val wave = remember(movie.id) { PosterWave() }
                Column(Modifier.width(posterWidth)) {
                    Box(
                        Modifier
                            .width(posterWidth)
                            .height(posterHeight)
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            },
                    ) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .clip(PosterShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                        ) {
                        PosterImage(
                            url = movie.posterUrl,
                            movieId = movie.id,
                            motion = motion,
                            motionPlace = motionPlace,
                            playTap = playTap,
                            contentDescription = stringResource(R.string.poster_content_description, movie.title),
                            modifier = Modifier
                                .matchParentSize()
                                .posterWave(wave)
                                .holdToPeek(
                                    onPeek = { onMoviePeek(movie) },
                                    onProgress = { holdProgress = it },
                                    onPressed = { pressed = it },
                                    onClick = {
                                        poster?.invoke()
                                        DetailSeed.put(movie)
                                        onMovieClick(movie.id)
                                    },
                                    onCorner = {
                                        val play = playTap.play ?: return@holdToPeek false
                                        play()
                                        true
                                    },
                                    wave = wave,
                                )
                                .holdCue(holdProgress),
                        )
                        val fraction = progressOf(movie)
                        val caption = captionOf(movie)
                        if (fraction != null || !caption.isNullOrBlank()) {
                            WatchProgress(
                                fraction = fraction ?: 0f,
                                caption = caption,
                                modifier = Modifier.align(Alignment.BottomCenter),
                            )
                        }
                        overlay(movie)
                        }
                        if (movie.id in watched) {
                            WatchedBadge(Modifier.align(Alignment.BottomEnd))
                        } else if (movie.id in favorites) {
                            FavoriteBadge(Modifier.align(Alignment.BottomEnd))
                        }
                    }
                    Spacer(Modifier.height(WatchedBadgeHang))
                    PosterCaption(
                        movie.title,
                        movie.year,
                        movie.releaseDate,
                        yearColor = if (yearWhite) Color.White else CinemaMuted,
                    )
                }
            }
            if (onOpen != null) {
                item(key = "rail-see-all") {
                    RailSeeAllCard(
                        width = trailWidth,
                        height = posterHeight,
                        glass = seeAllGlass,
                        onClick = {
                            tick?.invoke()
                            onOpen()
                        },
                    )
                }
            }
        }
        }
    }
}

/** Ретушь на стороне, куда уходит контент: влево → слева, вправо → справа. */
private fun Modifier.railSideRetouch(
    ink: Color,
    width: Dp,
    left: Float,
    right: Float,
): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val leftAmount = left.coerceIn(0f, 1f)
            val rightAmount = right.coerceIn(0f, 1f)
            if (leftAmount <= 0.01f && rightAmount <= 0.01f) return@drawWithContent
            val w = width.toPx()
            if (w < 1f) return@drawWithContent
            drawRect(
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 1f - leftAmount),
                        (w / size.width).coerceIn(0f, 0.45f) to Color.Black,
                        (1f - w / size.width).coerceIn(0.55f, 1f) to Color.Black,
                        1f to Color.Black.copy(alpha = 1f - rightAmount),
                    ),
                ),
                blendMode = BlendMode.DstIn,
            )
            if (leftAmount > 0.01f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to ink.copy(alpha = 0.78f * leftAmount),
                            0.55f to ink.copy(alpha = 0.3f * leftAmount),
                            1f to Color.Transparent,
                        ),
                        endX = w,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6A7A9A).copy(alpha = 0.28f * leftAmount),
                            Color.Transparent,
                        ),
                        center = Offset(0f, size.height * 0.45f),
                        radius = w * 1.4f,
                    ),
                    blendMode = BlendMode.Overlay,
                )
            }
            if (rightAmount > 0.01f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.45f to ink.copy(alpha = 0.3f * rightAmount),
                            1f to ink.copy(alpha = 0.78f * rightAmount),
                        ),
                        startX = size.width - w,
                        endX = size.width,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF6A7A9A).copy(alpha = 0.28f * rightAmount),
                            Color.Transparent,
                        ),
                        center = Offset(size.width, size.height * 0.45f),
                        radius = w * 1.4f,
                    ),
                    blendMode = BlendMode.Overlay,
                )
            }
        }

@Composable
internal fun RailSeeAllCard(
    width: Dp,
    height: Dp,
    glass: Boolean,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val pressScale = rememberPressScale(pressed)
    val wave = remember { PosterWave() }
    Box(
        modifier = Modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = stringResource(R.string.rail_see_all),
            color = Color.White,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .clip(RoundedCornerShape(percent = 50))
                .background(Color.White.copy(alpha = if (glass) 0.05f else 0.16f))
                .posterWave(wave)
                .pressToOpen(
                    onPressed = { pressed = it },
                    onClick = onClick,
                    wave = wave,
                )
                .padding(horizontal = 28.dp, vertical = 14.dp),
        )
    }
}
