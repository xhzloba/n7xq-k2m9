package com.example.movie.presentation.favorites

import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.movie.presentation.home.PeekEnter
import com.example.movie.presentation.home.PeekExit
import com.example.movie.R
import com.example.movie.presentation.common.PosterImage
import com.example.movie.presentation.detail.DetailSeed
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.toMovie
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.domain.model.HapticSlot
import com.example.movie.presentation.haptic.rememberHaptic
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.PosterCaption
import com.example.movie.presentation.home.components.WatchedBadge
import com.example.movie.presentation.home.components.WatchedBadgeHang
import com.example.movie.presentation.home.components.rememberWatchedIds
import com.example.movie.presentation.home.components.WatchProgress
import com.example.movie.presentation.home.components.posterTitlesOn
import com.example.movie.presentation.home.components.MenuSombreroEffect
import com.example.movie.presentation.home.components.CategoryTabFontSize
import com.example.movie.presentation.home.components.CategoryTabLetterSpacing
import com.example.movie.presentation.home.components.CategoryTabLineHeight
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface
import com.example.movie.ui.theme.PosterShape

@Composable
internal fun List<SavedTitle>.genreLabels(): List<String> {
    val labels = LinkedHashMap<String, String>()
    for (item in this) {
        item.genres.split(',').forEach { raw ->
            val label = raw.trim()
            if (label.isEmpty()) return@forEach
            labels.putIfAbsent(label.lowercase(), label)
        }
    }
    return labels.values.toList()
}

internal fun SavedTitle.genreKeys(): Set<String> =
    genres.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

internal fun List<SavedTitle>.sortedFor(sort: LibrarySort?): List<SavedTitle> = when (sort) {
    null -> this
    LibrarySort.Title -> sortedBy { it.title.lowercase() }
    LibrarySort.Year -> sortedWith(
        compareByDescending<SavedTitle> { it.year != null }
            .thenByDescending { it.year ?: 0 }
            .thenBy { it.title.lowercase() },
    )
    LibrarySort.Genre -> sortedWith(
        compareBy<SavedTitle> { it.genres.substringBefore(',').trim().lowercase() }
            .thenBy { it.title.lowercase() },
    )
}

@Composable
internal fun FavoriteTabs(
    selected: FavoriteTab,
    genres: List<String>,
    genre: String?,
    filtersOpen: Boolean,
    onSelect: (FavoriteTab) -> Unit,
    onOpenFilters: () -> Unit,
    onMenuSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    MenuSombreroEffect(listState, onMenuSettled)
    HapticOnChange(listState) { listState.passedTab() }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PosterGridInset),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(FavoriteTab.entries, key = { it.name }) { tab ->
            val lit = tab == selected
            val title = stringResource(tab.label)
            val picked = genres.firstOrNull { it.lowercase() == genre }
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember(tab.name) { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (tab != selected) onSelect(tab)
                        else if (tab.hasGenres) onOpenFilters()
                    },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(title)
                        if (lit && picked != null) {
                            withStyle(SpanStyle(color = CapsuleInk, fontWeight = FontWeight.Normal)) {
                                append(" — $picked")
                            }
                        }
                    },
                    color = if (lit) Color.White else Color(0xFF8A8A90),
                    fontWeight = if (lit) FontWeight.Bold else FontWeight.Normal,
                    fontSize = CategoryTabFontSize,
                    lineHeight = CategoryTabLineHeight,
                    letterSpacing = CategoryTabLetterSpacing,
                )
                if (tab.hasGenres && lit) {
                    MenuArrow(
                        open = filtersOpen && lit,
                        tint = if (lit && picked == null) Color.White else CapsuleInk,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LibraryFilterDialog(
    open: Boolean,
    title: String,
    genres: List<String>,
    genre: String?,
    sort: LibrarySort?,
    onGenre: (String?) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onDismiss: () -> Unit,
) {
    val visible = remember { MutableTransitionState(false) }
    LaunchedEffect(open) { visible.targetState = open }
    if (!visible.currentState && !visible.targetState) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setWindowAnimations(0)
                setDimAmount(0f)
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }
        AnimatedVisibility(
            visibleState = visible,
            enter = PeekEnter,
            exit = PeekExit,
            modifier = Modifier.fillMaxSize(),
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 22.dp)
                        .fillMaxWidth()
                        .clip(FilterShape)
                        .background(CinemaSurface)
                        .border(0.5.dp, Color.White.copy(alpha = 0.10f), FilterShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.favorites_sort),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        color = CinemaMuted,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LibrarySort.entries.forEach { option ->
                            FilterChip(
                                label = stringResource(option.label),
                                chosen = option == sort,
                                onClick = { onSort(option) },
                            )
                        }
                    }
                    if (genres.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )
                        Text(
                            text = stringResource(R.string.favorites_genres),
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            color = CinemaMuted,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                genres.forEach { label ->
                                    FilterChip(
                                        label = label,
                                        chosen = label.lowercase() == genre,
                                        onClick = {
                                            onGenre(if (label.lowercase() == genre) null else label.lowercase())
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

@Composable
internal fun FilterChip(
    label: String,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (chosen) Color.White else FilterNotes,
        fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (chosen) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember(label) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
internal fun MenuArrow(
    open: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(220),
        label = "menuArrow",
    )
    Icon(
        imageVector = Icons.Filled.KeyboardArrowDown,
        contentDescription = stringResource(R.string.favorites_genres),
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
    )
}

internal fun LazyListState.passedTab(): Int {
    val head = layoutInfo.visibleItemsInfo.firstOrNull() ?: return firstVisibleItemIndex
    return if (head.offset + head.size / 2 < 0) head.index + 1 else head.index
}

@Composable
internal fun FavoritePoster(
    item: SavedTitle,
    badge: String? = null,
    progress: Float? = null,
    caption: String? = null,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val tick = rememberHaptic(HapticSlot.Poster)
    var holdProgress by remember(item.id) { mutableFloatStateOf(0f) }
    var pressed by remember(item.id) { mutableStateOf(false) }
    val pressScale = rememberPressScale(pressed)
    val wave = remember(item.id) { PosterWave() }
    val showTitles = posterTitlesOn()
    val watched = rememberWatchedIds()
    Column {
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / 1.46f)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clip(PosterShape)
                    .background(Color.White.copy(alpha = 0.06f)),
            ) {
                PosterImage(
                    url = item.posterUrl,
                    movieId = item.id,
                    contentDescription = stringResource(R.string.poster_content_description, item.title),
                    modifier = Modifier
                        .matchParentSize()
                        .posterWave(wave)
                        .holdToPeek(
                            onPeek = { onMoviePeek(item.toMovie()) },
                            onProgress = { holdProgress = it },
                            onPressed = { pressed = it },
                            onClick = {
                                tick?.invoke()
                                DetailSeed.put(item.toMovie())
                                onMovieClick(item.id)
                            },
                            wave = wave,
                        )
                        .holdCue(holdProgress),
                )
                if (progress != null || !caption.isNullOrBlank()) {
                    WatchProgress(
                        fraction = progress ?: 0f,
                        caption = caption,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            if (item.id in watched) {
                WatchedBadge(Modifier.align(Alignment.BottomEnd))
            }
            if (badge != null) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(WatchedBadgeHang))
        if (showTitles) PosterCaption(item.title, item.year)
    }
}

