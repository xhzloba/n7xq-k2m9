package com.example.movie.tvui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.presentation.detail.drawPlexWash
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.PosterShape
import kotlinx.coroutines.delay

internal enum class TvPlace(
    @StringRes val label: Int,
    val kind: String? = null,
    val icon: ImageVector? = null,
    @DrawableRes val mark: Int = 0,
    val color: Color = Color.White,
) {
    Home(R.string.nav_home, icon = ShellIcons.home, color = Color(0xFF3B6EF5)),
    Movie(R.string.catalog_movies, "movie"),
    Serial(R.string.catalog_series, "serial"),
    Cartoon(R.string.catalog_cartoons, "multfilm"),
    CartoonSeries(R.string.catalog_cartoon_series, "multserial"),
    Anime(R.string.catalog_anime, "anime"),
    Kinopoisk(R.string.nav_kinopoisk, mark = R.drawable.ic_nav_kinopoisk, color = Color(0xFFFF6600)),
    Search(R.string.nav_search, icon = ShellIcons.search, color = Color.White),
    Settings(R.string.nav_settings, icon = ShellIcons.settings, color = Color(0xFFD4920A)),
}

private enum class TvSlot(
    @StringRes val label: Int,
    val icon: ImageVector? = null,
    @DrawableRes val mark: Int = 0,
    val color: Color = Color.White,
    val place: TvPlace? = null,
) {
    Home(R.string.nav_home, icon = ShellIcons.home, color = Color(0xFF3B6EF5), place = TvPlace.Home),
    Catalog(R.string.nav_catalog, icon = ShellIcons.catalog, color = Color(0xFF14A36A)),
    Kinopoisk(R.string.nav_kinopoisk, mark = R.drawable.ic_nav_kinopoisk, color = Color(0xFFFF6600), place = TvPlace.Kinopoisk),
    Search(R.string.nav_search, icon = ShellIcons.search, color = Color.White, place = TvPlace.Search),
    Settings(R.string.nav_settings, icon = ShellIcons.settings, color = Color(0xFFD4920A), place = TvPlace.Settings),
}

internal const val TvSlotCount = 5

internal val TvPlace.slot: Int
    get() = when (this) {
        TvPlace.Home -> 0
        TvPlace.Movie, TvPlace.Serial, TvPlace.Cartoon, TvPlace.CartoonSeries, TvPlace.Anime -> 1
        TvPlace.Kinopoisk -> 2
        TvPlace.Search -> 3
        TvPlace.Settings -> 4
    }

private val Kinds = listOf(
    TvPlace.Movie,
    TvPlace.Serial,
    TvPlace.Cartoon,
    TvPlace.CartoonSeries,
    TvPlace.Anime,
)

private val IdleInk = Color(0xFFC7C7CC)
private val PillShape = RoundedCornerShape(percent = 50)
private val MenuPlate = Color(0xFF0C0C0C)
private val PlateEdge = Color.White.copy(alpha = 0.16f)
private val PearlStroke = Brush.linearGradient(
    0f to Color(0xFFE6E1D8),
    0.5f to Color(0xFFB4AFA8),
    1f to Color(0xFFF1EDE6),
)
private val ClearStroke = Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
private val CardWidth = 208.dp
internal val TvPillStart = 16.dp
private val PillPad = 5.dp
private val PillWell = 26.dp
internal val TvRailInset = TvPillStart + PillPad * 2 + PillWell + TvPillStart

@Composable
internal fun TvSideMenu(
    current: TvPlace,
    compact: Boolean,
    reach: Int,
    focus: List<FocusRequester>,
    onOpen: (TvPlace) -> Unit,
    onDismiss: () -> Unit,
    onMenuFocus: (Boolean) -> Unit,
    backdrop: TvBackdrop,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var catalogRow by remember { mutableStateOf(false) }
    var flyoutRow by remember { mutableStateOf(false) }
    val flyout = catalogRow || flyoutRow
    val density = LocalDensity.current
    val awayPx = with(density) { -(CardWidth + 320.dp).toPx() }
    val plateX = remember { Animatable(-4000f) }
    var plateSeen by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) {
            plateSeen = true
            if (plateX.value < awayPx) plateX.snapTo(awayPx)
            plateX.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
        } else if (plateSeen) {
            plateX.animateTo(awayPx, tween(260, easing = FastOutSlowInEasing))
            catalogRow = false
            flyoutRow = false
        } else {
            plateX.snapTo(awayPx)
        }
    }
    val plateLive = open || plateX.value > awayPx + 1f
    val shown by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(260),
        label = "tv-menu",
    )
    val travel by animateFloatAsState(
        targetValue = if (compact && !open) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "pill",
    )
    val well = lerp(PillWell, 16.dp, travel)
    val iconSize = lerp(15.dp, 9.dp, travel)
    val kindFocus = remember { Kinds.map { FocusRequester() } }
    var originY by remember { mutableFloatStateOf(0f) }
    var catalogY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(reach) {
        if (reach == 0) return@LaunchedEffect
        closing = false
        open = true
        val target = focus[current.slot]
        repeat(8) {
            val moved = runCatching { target.requestFocus() }
            if (moved.isSuccess && moved.getOrNull() != false) return@LaunchedEffect
            delay(32)
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .onFocusChanged {
                if (!it.hasFocus) open = false
                else if (!closing) open = true
                onMenuFocus(it.hasFocus && !closing)
            }
            .onGloballyPositioned { originY = it.positionInRoot().y },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown }
                .drawBehind { drawPlexWash(backdrop.wash, 0.74f) },
        )
        if (!plateLive) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = TvPillStart)
                    .clip(PillShape)
                    .background(Color.Black.copy(alpha = 0.38f + (0.1f - 0.38f) * travel))
                    .padding(
                        horizontal = lerp(PillPad, 3.dp, travel),
                        vertical = lerp(7.dp, 4.dp, travel),
                    ),
                verticalArrangement = Arrangement.spacedBy(lerp(2.dp, 1.dp, travel)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TvSlot.entries.forEachIndexed { index, slot ->
                    SlotIcon(
                        slot = slot,
                        lit = slot.owns(current),
                        size = well,
                        icon = iconSize,
                        focus = focus[index],
                        allow = false,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { translationX = plateX.value }
                    .padding(start = 22.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxHeight()
                    .width(CardWidth)
                    .border(1.dp, PlateEdge, PosterShape)
                    .clip(PosterShape)
                    .background(MenuPlate)
                    .padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TvSlot.entries.forEachIndexed { index, slot ->
                    val rowFocus = focus[index]
                    var focused by remember(slot) { mutableStateOf(false) }
                    val openKind = {
                        val picked = Kinds.indexOf(current).coerceAtLeast(0)
                        runCatching { kindFocus[picked].requestFocus() }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .onGloballyPositioned {
                                if (slot == TvSlot.Catalog) catalogY = it.positionInRoot().y
                            }
                            .focusRequester(rowFocus)
                            .onFocusChanged {
                                focused = it.isFocused
                                if (slot == TvSlot.Catalog) catalogRow = it.isFocused
                            }
                            .onPreviewKeyEvent { event ->
                                if (!focused) return@onPreviewKeyEvent false
                                val native = event.nativeKeyEvent
                                val right = event.key == Key.DirectionRight
                                val enter = event.key == Key.DirectionCenter ||
                                    event.key == Key.Enter ||
                                    event.key == Key.NumPadEnter
                                val back = event.key == Key.Back || event.key == Key.Escape
                                if (!right && !enter && !back) return@onPreviewKeyEvent false
                                if (event.type != KeyEventType.KeyDown || native.repeatCount > 0 || native.isLongPress) {
                                    return@onPreviewKeyEvent true
                                }
                                when {
                                    back -> {
                                        closing = true
                                        open = false
                                        onDismiss()
                                    }
                                    slot == TvSlot.Catalog && (right || enter) -> openKind()
                                    right || enter -> slot.place?.let {
                                        closing = true
                                        open = false
                                        onOpen(it)
                                    }
                                }
                                true
                            }
                            .focusProperties {
                                canFocus = open
                                left = FocusRequester.Cancel
                                right = if (slot == TvSlot.Catalog) kindFocus[Kinds.indexOf(current).coerceAtLeast(0)] else FocusRequester.Cancel
                                if (index == 0) up = FocusRequester.Cancel
                                if (index == TvSlot.entries.lastIndex) down = FocusRequester.Cancel
                            }
                            .focusable()
                            .border(
                                width = if (focused) 1.dp else 0.dp,
                                brush = if (focused) PearlStroke else ClearStroke,
                                shape = PillShape,
                            )
                            .clip(PillShape)
                            .clickable(
                                interactionSource = remember(slot) { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (slot == TvSlot.Catalog) openKind() else slot.place?.let(onOpen)
                                },
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SlotGlyph(slot, if (slot.owns(current) || focused) slot.color else IdleInk, 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(slot.label),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        if (slot == TvSlot.Catalog) {
                            Icon(
                                imageVector = ShellIcons.chevron,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            if (flyout) {
                val flyTop = with(density) { (catalogY - originY).toDp() }
                Column(
                    modifier = Modifier
                        .graphicsLayer { translationX = plateX.value }
                        .padding(start = 22.dp + CardWidth + 10.dp, top = flyTop)
                        .width(196.dp)
                        .border(1.dp, PlateEdge, PosterShape)
                        .clip(PosterShape)
                        .background(MenuPlate)
                        .onFocusChanged { flyoutRow = it.hasFocus }
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Kinds.forEachIndexed { index, kind ->
                        var focused by remember(kind) { mutableStateOf(false) }
                        val commit = { onOpen(kind) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .focusRequester(kindFocus[index])
                                .onFocusChanged { focused = it.isFocused }
                                .onPreviewKeyEvent { event ->
                                    if (!focused) return@onPreviewKeyEvent false
                                    val native = event.nativeKeyEvent
                                    val ok = event.key == Key.DirectionCenter ||
                                        event.key == Key.Enter ||
                                        event.key == Key.NumPadEnter
                                    val right = event.key == Key.DirectionRight
                                    val back = event.key == Key.DirectionLeft ||
                                        event.key == Key.Back ||
                                        event.key == Key.Escape
                                    if (!ok && !right && !back) return@onPreviewKeyEvent false
                                    if (event.type != KeyEventType.KeyDown || native.repeatCount > 0 || native.isLongPress) {
                                        return@onPreviewKeyEvent true
                                    }
                                    when {
                                        back -> runCatching { focus[TvSlot.Catalog.ordinal].requestFocus() }
                                        ok -> {
                                            closing = true
                                            open = false
                                            commit()
                                        }
                                    }
                                    true
                                }
                                .focusProperties {
                                    canFocus = flyout
                                    left = focus[TvSlot.Catalog.ordinal]
                                    right = FocusRequester.Cancel
                                    if (index == 0) up = FocusRequester.Cancel
                                    if (index == Kinds.lastIndex) down = FocusRequester.Cancel
                                }
                                .focusable()
                                .border(
                                    width = if (focused) 1.dp else 0.dp,
                                    brush = if (focused) PearlStroke else ClearStroke,
                                    shape = PillShape,
                                )
                                .clip(PillShape)
                                .clickable(
                                    interactionSource = remember(kind) { MutableInteractionSource() },
                                    indication = null,
                                    onClick = commit,
                                )
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = stringResource(kind.label),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun TvSlot.owns(place: TvPlace): Boolean = when (this) {
    TvSlot.Catalog -> place.kind != null
    else -> this.place == place
}

@Composable
private fun SlotIcon(
    slot: TvSlot,
    lit: Boolean,
    size: androidx.compose.ui.unit.Dp,
    icon: androidx.compose.ui.unit.Dp,
    focus: FocusRequester,
    allow: Boolean,
) {
    Box(
        modifier = Modifier
            .size(size)
            .focusRequester(focus)
            .focusProperties { canFocus = allow }
            .focusable()
            .clip(CircleShape)
            .background(if (lit) slot.color.copy(alpha = 0.22f) else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        SlotGlyph(slot, if (lit) slot.color else IdleInk, icon)
    }
}

@Composable
private fun SlotGlyph(slot: TvSlot, tint: Color, size: androidx.compose.ui.unit.Dp) {
    val description = stringResource(slot.label)
    val glyph = Modifier.size(size)
    if (slot.mark != 0) {
        Icon(painterResource(slot.mark), description, tint = tint, modifier = glyph)
    } else if (slot.icon != null) {
        Icon(slot.icon, description, tint = tint, modifier = glyph)
    }
}
