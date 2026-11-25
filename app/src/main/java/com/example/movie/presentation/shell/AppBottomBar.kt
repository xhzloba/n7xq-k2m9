package com.example.movie.presentation.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.presentation.common.appChrome
import com.example.movie.presentation.common.dockInk
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.tv.dpadSkip
import com.example.movie.presentation.update.LocalAppUpdate
import com.example.movie.presentation.update.UpdateBadge
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

internal val WashDockFallback = Color(0xFF121212)

private val IdleInk = Color(0xFFC7C7CC)

@Composable
internal fun AppBottomBar(
    selected: AppTab?,
    onSelect: (AppTab) -> Unit,
    onReselect: (AppTab) -> Unit,
    onHomeHold: () -> Unit,
    onLibraryHold: () -> Unit,
    onHomeAnchor: (Rect) -> Unit,
    washLeft: Color? = null,
    washRight: Color? = null,
    washAlpha: Float = 1f,
    clear: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val press = ripple(color = Color(0xFF8E8E93))
    val updatePending = LocalAppUpdate.current.pending
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val showLabels = settings.navLabels
    val iconSize = if (showLabels) 24.dp else 28.dp
    // Слот иконка+подпись: 24 + 5 + 13 ≈ 42; без подписи иконка крупнее, слот тот же — выравнивание сверху.
    val slotHeight = 42.dp
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val belowGesture = (nav + 4.dp).coerceAtLeast(22.dp)
    val leftTarget = (washLeft ?: WashDockFallback).dockInk()
    val rightTarget = (washRight ?: washLeft ?: WashDockFallback).dockInk()
    val left by animateColorAsState(
        targetValue = leftTarget,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "barDockL",
    )
    val right by animateColorAsState(
        targetValue = rightTarget,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "barDockR",
    )
    val washOn = washLeft != null && !clear
    val washIn = washAlpha.coerceIn(0f, 1f)
    // Пока фон проявляется — сразу целевой цвет. После 1f лист полок идёт через animateColorAsState.
    val inkLeft = if (washOn && washIn < 0.995f) leftTarget else left
    val inkRight = if (washOn && washIn < 0.995f) rightTarget else right
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (clear || (washOn && washIn >= 0.995f)) Color.Transparent else appChrome())
            .drawBehind {
                if (clear) return@drawBehind
                if (washOn && washIn > 0.01f) {
                    val layer = washIn < 0.995f
                    if (layer) {
                        drawContext.canvas.saveLayer(
                            Rect(0f, 0f, size.width, size.height),
                            Paint().apply { alpha = washIn },
                        )
                    }
                    drawRect(brush = Brush.horizontalGradient(listOf(inkLeft, inkRight)))
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.22f),
                            0.4f to Color.Black.copy(alpha = 0.34f),
                            1f to Color.Black.copy(alpha = 0.46f),
                        ),
                    )
                    if (layer) drawContext.canvas.restore()
                }
                val stroke = 0.6.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = if (washOn) 0.28f + (0.16f - 0.28f) * washIn else 0.28f),
                    start = Offset(0f, stroke / 2f),
                    end = Offset(size.width, stroke / 2f),
                    strokeWidth = stroke,
                )
            }
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = belowGesture),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTab.entries.forEach { tab ->
            val lit = tab == selected
            val badge = tab == AppTab.Settings && updatePending
            val interaction = remember(tab) { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val animateIcons = !showLabels
            val pressScale by animateFloatAsState(
                targetValue = if (animateIcons && pressed) 0.86f else 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessHigh,
                ),
                label = "navPress",
            )
            val selectBounce = remember(tab) { Animatable(1f) }
            LaunchedEffect(lit, animateIcons) {
                if (!animateIcons) {
                    selectBounce.snapTo(1f)
                    return@LaunchedEffect
                }
                if (lit) {
                    selectBounce.snapTo(0.72f)
                    selectBounce.animateTo(
                        1f,
                        spring(
                            dampingRatio = 0.36f,
                            stiffness = 520f,
                        ),
                    )
                } else {
                    selectBounce.snapTo(1f)
                }
            }
            val iconScale = if (animateIcons) pressScale * selectBounce.value else 1f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .onGloballyPositioned { coords ->
                        if (tab == AppTab.Home) onHomeAnchor(coords.boundsInRoot())
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(slotHeight)
                        .clip(RoundedCornerShape(22.dp))
                        .combinedClickable(
                            interactionSource = interaction,
                            indication = press,
                            onLongClick = when (tab) {
                                AppTab.Home -> {
                                    {
                                        tick?.invoke()
                                        onHomeHold()
                                    }
                                }
                                AppTab.Favorites -> {
                                    {
                                        tick?.invoke()
                                        onLibraryHold()
                                    }
                                }
                                else -> null
                            },
                            onClick = {
                                if (tab != selected) {
                                    tick?.invoke()
                                    onSelect(tab)
                                } else {
                                    tick?.invoke()
                                    onReselect(tab)
                                }
                            },
                        )
                        .padding(top = if (showLabels) 0.dp else 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                ) {
                    val iconMod = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                    val ink = if (lit) tab.color else IdleInk
                    if (tab.mark != 0) {
                        Icon(
                            painter = painterResource(tab.mark),
                            contentDescription = stringResource(tab.label),
                            tint = ink,
                            modifier = iconMod,
                        )
                    } else {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.label),
                            tint = ink,
                            modifier = iconMod,
                        )
                    }
                    if (showLabels) {
                        Text(
                            text = stringResource(tab.label),
                            modifier = Modifier.padding(top = 5.dp),
                            color = if (lit) tab.color else IdleInk,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (badge) {
                    UpdateBadge(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(x = 16.dp, y = (-2).dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeHoldHint(
    anchor: Rect,
    active: Boolean,
) {
    val open = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            delay(1_500)
            open.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        } else {
            open.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
        }
    }
    val progress = open.value
    if (progress <= 0.001f) return
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        val center = Offset(
            anchor.left - origin.x + anchor.width / 2f,
            anchor.top - origin.y + anchor.height / 2f,
        )
        val radius = with(density) { 30.dp.toPx() }
        val veil = appChrome().copy(alpha = 0.9f * progress)
        val pulseTransition = rememberInfiniteTransition(label = "home-hint-glow")
        val pulse by pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "home-hint-pulse",
        )
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addOval(
                    Rect(
                        center.x - radius,
                        center.y - radius,
                        center.x + radius,
                        center.y + radius,
                    ),
                )
            }
            drawPath(path, veil)
            if (progress > 0.05f) {
                val spread = 10.dp.toPx() + 12.dp.toPx() * pulse
                val glow = (0.1f + 0.16f * pulse) * progress
                drawCircle(
                    brush = Brush.radialGradient(
                        0.5f to Color.White.copy(alpha = glow),
                        1f to Color.Transparent,
                        center = center,
                        radius = radius + spread,
                    ),
                    radius = radius + spread,
                    center = center,
                )
                drawCircle(
                    color = Color.White.copy(alpha = glow),
                    radius = radius + spread * 0.35f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx()),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.72f * progress),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        val textAlpha = progress
        if (textAlpha > 0f) {
            val above = with(density) {
                (constraints.maxHeight - (center.y - radius) + 16.dp.toPx()).toDp()
            }
            Text(
                text = stringResource(R.string.home_hold_hint),
                color = Color.White.copy(alpha = textAlpha),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(0f, 1f),
                        blurRadius = 12f,
                    ),
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 32.dp, end = 32.dp, bottom = above)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = (1f - textAlpha) * 12.dp.toPx() },
            )
        }
        val top = (center.y - radius).coerceAtLeast(0f)
        val bottom = (constraints.maxHeight - center.y - radius).coerceAtLeast(0f)
        val left = (center.x - radius).coerceAtLeast(0f)
        val right = (constraints.maxWidth - center.x - radius).coerceAtLeast(0f)
        val band = (radius * 2f).coerceAtMost((constraints.maxHeight - top).coerceAtLeast(0f))
        if (top > 1f) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(with(density) { top.toDp() })
                    .eatClick(),
            )
        }
        if (bottom > 1f) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(with(density) { bottom.toDp() })
                    .eatClick(),
            )
        }
        if (left > 1f && band > 1f) {
            Box(
                Modifier
                    .offset { IntOffset(0, top.roundToInt()) }
                    .width(with(density) { left.toDp() })
                    .height(with(density) { band.toDp() })
                    .eatClick(),
            )
        }
        if (right > 1f && band > 1f) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, top.roundToInt()) }
                    .width(with(density) { right.toDp() })
                    .height(with(density) { band.toDp() })
                    .eatClick(),
            )
        }
    }
}

@Composable
internal fun CatalogShelfHint(
    anchor: Rect,
    active: Boolean,
) {
    val open = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            delay(1_500)
            open.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
        } else {
            open.snapTo(0f)
        }
    }
    val progress = open.value
    if (progress <= 0.001f) return
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        val padX = with(density) { 14.dp.toPx() }
        val padY = with(density) { 8.dp.toPx() }
        val hole = Rect(
            anchor.left - origin.x - padX,
            anchor.top - origin.y - padY,
            anchor.right - origin.x + padX,
            anchor.bottom - origin.y + padY,
        )
        val corner = CornerRadius(hole.height / 2f, hole.height / 2f)
        val veil = appChrome().copy(alpha = 0.9f * progress)
        val pulseTransition = rememberInfiniteTransition(label = "catalog-hint-glow")
        val pulse by pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "catalog-hint-pulse",
        )
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, size.width, size.height))
                addRoundRect(RoundRect(hole, corner))
            }
            drawPath(path, veil)
            if (progress > 0.05f) {
                val spread = 8.dp.toPx() + 10.dp.toPx() * pulse
                val glow = (0.1f + 0.16f * pulse) * progress
                val grown = Rect(
                    hole.left - spread,
                    hole.top - spread * 0.65f,
                    hole.right + spread,
                    hole.bottom + spread * 0.65f,
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = glow),
                    topLeft = androidx.compose.ui.geometry.Offset(grown.left, grown.top),
                    size = androidx.compose.ui.geometry.Size(grown.width, grown.height),
                    cornerRadius = CornerRadius(grown.height / 2f, grown.height / 2f),
                    style = Stroke(width = 3.dp.toPx()),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.72f * progress),
                    topLeft = androidx.compose.ui.geometry.Offset(hole.left, hole.top),
                    size = androidx.compose.ui.geometry.Size(hole.width, hole.height),
                    cornerRadius = corner,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        val textAlpha = progress
        if (textAlpha > 0f) {
            Text(
                text = stringResource(R.string.catalog_fill_hint),
                color = Color.White.copy(alpha = textAlpha),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.85f),
                        offset = Offset(0f, 1f),
                        blurRadius = 12f,
                    ),
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 32.dp, end = 32.dp, bottom = 88.dp)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = (1f - textAlpha) * 12.dp.toPx() },
            )
        }
        val top = hole.top.coerceAtLeast(0f)
        val bottom = (constraints.maxHeight - hole.bottom).coerceAtLeast(0f)
        val left = hole.left.coerceAtLeast(0f)
        val right = (constraints.maxWidth - hole.right).coerceAtLeast(0f)
        val band = hole.height.coerceAtMost((constraints.maxHeight - top).coerceAtLeast(0f))
        if (top > 1f) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(with(density) { top.toDp() })
                    .eatClick(),
            )
        }
        if (bottom > 1f) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(with(density) { bottom.toDp() })
                    .eatClick(),
            )
        }
        if (left > 1f && band > 1f) {
            Box(
                Modifier
                    .offset { IntOffset(0, top.roundToInt()) }
                    .width(with(density) { left.toDp() })
                    .height(with(density) { band.toDp() })
                    .eatClick(),
            )
        }
        if (right > 1f && band > 1f) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, top.roundToInt()) }
                    .width(with(density) { right.toDp() })
                    .height(with(density) { band.toDp() })
                    .eatClick(),
            )
        }
    }
}

@Composable
private fun Modifier.eatClick(): Modifier = this.dpadSkip().clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = {},
)
