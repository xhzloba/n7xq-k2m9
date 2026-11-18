@file:OptIn(ExperimentalFoundationApi::class)

package com.example.movie.presentation.home.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.pending
import com.example.movie.domain.model.Shelf
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.presentation.update.CountBadge
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue

internal val CategoryTabFontSize = 18.sp
internal val CategoryTabLineHeight = 24.sp
internal val CategoryTabLetterSpacing = (-0.2).sp

private val LogoHeight = 36.dp
internal val LogoAspect = 915f / 203f
internal val PosterGridInset = 16.dp
internal val HomeContentInset = PosterGridInset
internal val BrandTopInset = 16.dp
internal val BrandLogoBleed = 0.dp

private const val LeftPinnedShelves = 2

private val ShelfInk = Color.White

private val TabSkeletonLabels = listOf(
    "Популярное",
    "Сейчас смотрят",
    "4K",
    "Dolby Vision",
    "Обновления",
    "Фильмы",
)

@Composable
internal fun CategoryTabs(
    shelves: List<Shelf>,
    selectedId: String?,
    play: Animatable<Color, AnimationVector4D>,
    border: Color = Color.White.copy(alpha = 0.22f),
    onSelect: (String) -> Unit,
    onSearch: () -> Unit,
    onAlerts: () -> Unit = {},
    hat: Boolean = false,
    mustache: Boolean = false,
    onMenuSettled: () -> Unit = {},
    showBrand: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var centering by remember { mutableStateOf(false) }
    MenuSombreroEffect(listState, onMenuSettled) { centering }
    HapticOnChange(listState) { listState.passedTab() }
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val capsuleStyle = remember {
        TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = CategoryTabFontSize,
            lineHeight = CategoryTabLineHeight,
            letterSpacing = CategoryTabLetterSpacing,
        )
    }
    val capsulePad = with(density) { 0.dp.roundToPx() }
    val inset = with(density) { HomeContentInset.roundToPx() }
    LaunchedEffect(selectedId, shelves) {
        val index = shelves.indexOfFirst { it.id == selectedId }
        if (index < 0) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.viewportSize.width }.first { it > 0 }
        centering = true
        try {
            if (index < LeftPinnedShelves) {
                listState.alignStart()
            } else {
                val estimated = measurer.measure(
                    text = shelves[index].title,
                    style = capsuleStyle,
                    maxLines = 1,
                ).size.width + capsulePad
                listState.centerItem(index, estimated, inset)
            }
            snapshotFlow { listState.isScrollInProgress }.first { !it }
        } finally {
            centering = false
        }
    }
    BoxWithConstraints(modifier = modifier.graphicsLayer()) {
        val side = maxWidth / 2
        Column {
            if (showBrand) {
                BrandRow(
                    play = play.value,
                    onSearch = onSearch,
                    onAlerts = onAlerts,
                    hat = hat,
                    mustache = mustache,
                )
                Spacer(Modifier.height(12.dp))
            }
            CompositionLocalProvider(LocalOverscrollConfiguration provides OverscrollConfiguration()) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(start = HomeContentInset),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(shelves, key = { it.id }) { shelf ->
                    ShelfCapsule(
                        title = shelf.title,
                        selected = shelf.id == selectedId,
                        border = border,
                        onClick = { onSelect(shelf.id) },
                    )
                }
                item(key = "tail") { Spacer(Modifier.width(side)) }
            }
            }
        }
    }
}

private suspend fun LazyListState.alignStart() {
    if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) return
    animateScrollToItem(0)
}

private suspend fun LazyListState.centerItem(index: Int, estimated: Int, inset: Int) {
    val viewport = layoutInfo.viewportSize.width
    if (viewport <= 0) return
    val wasVisible = layoutInfo.visibleItemsInfo.any { it.index == index }
    if (!wasVisible) {
        animateScrollToItem(index, estimated / 2 + inset - viewport / 2)
    }
    val placed = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val delta = placed.offset + inset + placed.size / 2 - viewport / 2
    if (delta == 0) return
    if (wasVisible) {
        animateScrollBy(delta.toFloat())
    } else {
        scroll { scrollBy(delta.toFloat()) }
    }
}

private fun LazyListState.passedTab(): Int {
    val head = layoutInfo.visibleItemsInfo.firstOrNull() ?: return firstVisibleItemIndex
    return if (head.offset + head.size / 2 < 0) head.index + 1 else head.index
}

@Composable
private fun ShelfCapsule(
    title: String,
    selected: Boolean = false,
    border: Color = Color.White.copy(alpha = 0.22f),
    onClick: (() -> Unit)? = null,
    ghost: Boolean = false,
) {
    Column(
        modifier = Modifier
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = remember(title) { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                },
            )
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = if (ghost) Color.Transparent else if (selected) ShelfInk else CinemaMuted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = CategoryTabFontSize,
            lineHeight = CategoryTabLineHeight,
            letterSpacing = CategoryTabLetterSpacing,
            modifier = Modifier.drawBehind {
                if (!ghost) return@drawBehind
                val bar = 13.dp.toPx()
                val top = (size.height - bar) / 2f
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.10f),
                    topLeft = Offset(0f, top),
                    size = Size(size.width, bar),
                    cornerRadius = CornerRadius(bar / 2f, bar / 2f),
                )
            },
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .height(2.dp)
                .fillMaxWidth()
                .drawBehind {
                    if (!selected || ghost) return@drawBehind
                    drawRoundRect(
                        color = border,
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                    )
                },
        )
    }
}

@Composable
internal fun BrandRow(
    play: Color,
    onSearch: () -> Unit,
    onAlerts: () -> Unit = {},
    hat: Boolean = false,
    mustache: Boolean = false,
    blend: Animatable<Float, AnimationVector1D>? = null,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = HomeContentInset - BrandLogoBleed, end = HomeContentInset - 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandWordmark(play = play, blend = blend, hat = hat, mustache = mustache)
        Spacer(Modifier.weight(1f))
        HeaderBell(onAlerts)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onSearch()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.search,
                contentDescription = stringResource(R.string.search),
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
internal fun HeaderBell(onClick: () -> Unit) {
    val tick = rememberHapticTick()
    val store = LocalAppContainer.current.releaseWatch.store
    val alerts by store.alerts.collectAsStateWithLifecycle()
    val shelves by store.shelves.collectAsStateWithLifecycle()
    val count = alerts.count { !it.read } + shelves.count { it.pending() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ShellIcons.bell,
            contentDescription = stringResource(R.string.nav_alerts),
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
        if (count > 0) {
            CountBadge(
                count = count,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 4.dp),
            )
        }
    }
}

@Composable
internal fun BrandWordmark(
    play: Color,
    hat: Boolean = false,
    mustache: Boolean = false,
    blend: Animatable<Float, AnimationVector1D>? = null,
) {
    val context = LocalContext.current
    val logo = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.lumen_logo, options)?.asImageBitmap()
    } ?: return
    Box(
        modifier = Modifier
            .height(LogoHeight)
            .aspectRatio(LogoAspect, matchHeightConstraintsFirst = true),
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .semantics { contentDescription = "Люмен" },
        ) {
            drawImage(
                image = logo,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.High,
            )
        }
    }
}

@Composable
internal fun MenuSombreroEffect(
    listState: LazyListState,
    onSettled: () -> Unit,
    suspended: () -> Boolean = { false },
) {
    SettleEffect(listState, { listState.isScrollInProgress && !suspended() }, onSettled)
}

@Composable
internal fun SettleEffect(key: Any, scrolling: () -> Boolean, onSettled: () -> Unit) {
    val settled by rememberUpdatedState(onSettled)
    LaunchedEffect(key) {
        var trailing = false
        snapshotFlow { scrolling() }
            .distinctUntilChanged()
            .collect { moving ->
                if (moving) {
                    trailing = true
                } else if (trailing) {
                    delay(140)
                    if (scrolling()) return@collect
                    trailing = false
                    settled()
                }
            }
    }
}

internal class LogoMood {
    var hat by mutableStateOf(false)
        private set
    var mustache by mutableStateOf(false)
        private set
    private var rides = 0

    fun onSettled() {
        if (!hat && !mustache) {
            if (Random.nextFloat() < 0.14f) {
                if (Random.nextBoolean()) hat = true else mustache = true
                rides = 0
            }
            return
        }
        rides += 1
        if (rides == 1) return
        val combo = hat && mustache
        if (!combo && Random.nextFloat() < 0.12f) {
            hat = true
            mustache = true
            rides = 0
            return
        }
        val leaves = when {
            combo && rides == 2 -> 0.3f
            combo -> 0.65f
            rides == 2 -> 0.25f
            else -> 0.55f
        }
        if (Random.nextFloat() < leaves) {
            if (combo && Random.nextFloat() < 0.5f) {
                if (Random.nextBoolean()) hat = false else mustache = false
            } else {
                hat = false
                mustache = false
            }
            rides = 0
        }
    }
}

@Composable
fun CategoryTabsSkeleton(
    onSearch: () -> Unit,
    onAlerts: () -> Unit = {},
    showBrand: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (showBrand) {
            BrandRow(play = PlayBlue, onSearch = onSearch, onAlerts = onAlerts)
            Spacer(Modifier.height(12.dp))
        }
        LazyRow(
            contentPadding = PaddingValues(start = HomeContentInset, end = HomeContentInset),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            userScrollEnabled = false,
        ) {
        items(TabSkeletonLabels.size) { index ->
            ShelfCapsule(
                title = TabSkeletonLabels[index],
                ghost = true,
            )
        }
        }
    }
}
