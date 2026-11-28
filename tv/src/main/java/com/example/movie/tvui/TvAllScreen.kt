package com.example.movie.tvui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.presentation.home.ShelfOverlapSkips
import com.example.movie.presentation.home.ShelfPages
import com.example.movie.di.AppContainer
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.detail.drawPlexWash
import com.example.movie.presentation.palette.PosterWash
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PosterShape
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val GridColumns = 6

/** Заготовка фона полного списка: чёрный, близкий сероватый отлив из левого верхнего угла. */
private val ShelfWash = PosterWash(
    topLeft = Color.White.copy(alpha = 0.16f),
    topRight = Color.White.copy(alpha = 0.045f),
    bottomRight = Color.White.copy(alpha = 0.02f),
    bottomLeft = Color.White.copy(alpha = 0.035f),
)
private const val LoadAhead = 8
private const val TitleLead = 1

/**
 * Вверх и вниз — одна пружина до стабильной точки.
 * Раньше вниз цель обнулялась, как только кадр влезал в экран:
 * доскролл обрывался, под годом оставалась пустая полоса.
 * На первом ряду якорь держит заголовок, без мгновенного scrollToItem.
 */
private class FlushEdge : BringIntoViewSpec {
    var anchor = 0f

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val delta = if (anchor > 0f) {
            offset - anchor
        } else {
            val trailing = offset + size
            val floor = containerSize - containerSize * 0.24f
            when {
                offset < -2f -> offset
                trailing > floor -> trailing - floor
                else -> 0f
            }
        }
        return if (kotlin.math.abs(delta) < 2f) 0f else delta
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TvAllPage(
    rail: TvRail,
    menuFocused: Boolean,
    returning: Int,
    posterTarget: Int,
    onPosterTarget: (Int) -> Unit,
    onOpen: (Movie) -> Unit,
    onReachMenu: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val model: TvShelfListViewModel = viewModel(
        key = rail.id,
        factory = TvShelfListViewModel.Factory(container, rail),
    )
    val movies = model.movies
    val grid = rememberLazyGridState()
    val edge = remember { FlushEdge() }
    val titleLead = with(LocalDensity.current) { 74.dp.toPx() }
    val requesters = remember(rail.id) { mutableStateListOf<FocusRequester>() }
    if (requesters.size < movies.size) {
        repeat(movies.size - requesters.size) { requesters += FocusRequester() }
    }

    var focusedIndex by remember { mutableIntStateOf(0) }
    var seenLast by remember { mutableIntStateOf(Int.MIN_VALUE) }
    LaunchedEffect(grid, movies.size, model.loading, model.end) {
        if (model.loading || model.end) return@LaunchedEffect
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1 }
            .distinctUntilChanged()
            .collect { last ->
                val moved = seenLast != Int.MIN_VALUE && last != seenLast
                seenLast = last
                if (last >= movies.size + TitleLead - LoadAhead) model.loadMore(force = moved)
            }
    }
    LaunchedEffect(movies.isNotEmpty(), menuFocused) {
        if (menuFocused || movies.isEmpty()) return@LaunchedEffect
        edge.anchor = titleLead
        repeat(24) {
            val target = requesters.getOrNull(0)
            if (target != null && runCatching { target.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(50)
        }
    }
    LaunchedEffect(returning) {
        if (menuFocused) return@LaunchedEffect
        val index = posterTarget.coerceIn(0, (movies.size - 1).coerceAtLeast(0))
        repeat(8) {
            val target = requesters.getOrNull(index) ?: return@repeat
            if (runCatching { target.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(40)
        }
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides edge) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GridColumns),
        state = grid,
        contentPadding = PaddingValues(start = TvRailInset, end = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(26.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawPlexWash(ShelfWash, 1f, intensity = 0.7f) }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionUp -> if (focusedIndex in GridColumns until GridColumns * 2) {
                            edge.anchor = titleLead
                        }
                        Key.DirectionDown -> if (focusedIndex < GridColumns) edge.anchor = 0f
                        else -> Unit
                    }
                }
                if (event.key != Key.Back && event.key != Key.Escape) return@onPreviewKeyEvent false
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
                onClose()
                true
            },
    ) {
        item(key = "title", span = { GridItemSpan(GridColumns) }) {
            Column(Modifier.padding(top = 40.dp)) {
                Text(
                    text = stringResource(rail.titleRes),
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.height(28.dp),
                )
                Spacer(Modifier.height(2.dp))
            }
        }
        itemsIndexed(movies, key = { _, movie -> movie.id }) { index, movie ->
                val column = index % GridColumns
                val row = index / GridColumns
                val lastRow = movies.lastIndex / GridColumns
                TvAllCell(
                    movie = movie,
                    topInset = if (row == 0) 0.dp else 18.dp,
                    focus = requesters[index],
                    allow = !menuFocused,
                    edgeLeft = column == 0,
                    edgeRight = column == GridColumns - 1 || index == movies.lastIndex,
                    edgeUp = row == 0,
                    edgeDown = row == lastRow || index + GridColumns > movies.lastIndex,
                    onOpen = { onOpen(movie) },
                    onFocus = {
                        focusedIndex = index
                        edge.anchor = if (index < GridColumns) titleLead else 0f
                        onPosterTarget(index)
                    },
                    onLeftEdge = {
                        onPosterTarget(index)
                        onReachMenu()
                    },
                )
        }
    }
    }
}

@Composable
private fun TvAllCell(
    movie: Movie,
    topInset: Dp,
    focus: FocusRequester,
    allow: Boolean,
    edgeLeft: Boolean,
    edgeRight: Boolean,
    edgeUp: Boolean,
    edgeDown: Boolean,
    onOpen: () -> Unit,
    onFocus: () -> Unit,
    onLeftEdge: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(160),
        label = "poster",
    )
    val cover = movie.posterUrl?.takeIf { it.isNotBlank() }
    var missing by remember(movie.id, cover) { mutableStateOf(cover == null) }
    Column(Modifier.fillMaxWidth().padding(top = topInset)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .focusRequester(focus)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocus()
                }
                .onPreviewKeyEvent { event ->
                    if (!focused) return@onPreviewKeyEvent false
                    val down = event.type == KeyEventType.KeyDown
                    val native = event.nativeKeyEvent
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (!edgeLeft) return@onPreviewKeyEvent false
                            if (!down || native.repeatCount > 0 || native.isLongPress) {
                                return@onPreviewKeyEvent true
                            }
                            onLeftEdge()
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (!down || native.repeatCount > 0 || native.isLongPress) {
                                return@onPreviewKeyEvent true
                            }
                            onOpen()
                            true
                        }
                        else -> false
                    }
                }
                .focusProperties {
                    canFocus = allow
                    if (edgeLeft) left = FocusRequester.Cancel
                    if (edgeRight) right = FocusRequester.Cancel
                    if (edgeUp) up = FocusRequester.Cancel
                    if (edgeDown) down = FocusRequester.Cancel
                }
                .focusable()
                .border(
                    width = 2.dp,
                    color = if (focused) Color.White else Color.Transparent,
                    shape = PosterShape,
                )
                .clip(PosterShape)
                .background(Color(0xFF2A2A2E))
                .clickable(onClick = onOpen),
            contentAlignment = Alignment.Center,
        ) {
            if (!missing) {
                AsyncImage(
                    model = cover,
                    contentDescription = stringResource(R.string.poster_content_description, movie.title),
                    contentScale = ContentScale.Crop,
                    onError = { missing = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (missing) {
                Icon(
                    imageVector = ShellIcons.trailers,
                    contentDescription = stringResource(R.string.poster_content_description, movie.title),
                    tint = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        val caption = TextStyle(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        )
        Text(
            text = movie.title,
            color = Color.White,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            style = caption,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
        if (movie.year != null && movie.year > 0) {
            Text(
                text = movie.year.toString(),
                color = CinemaMuted,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                style = caption,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp),
            )
        }
    }
}

internal class TvShelfListViewModel(
    private val container: AppContainer,
    private val rail: TvRail,
) : ViewModel() {
    private val pages = ShelfPages()
    var movies by mutableStateOf(rail.movies)
        private set
    var loading by mutableStateOf(false)
        private set
    var end by mutableStateOf(rail.query == null)
        private set
    private var queued = false
    private var failed = false

    init {
        if (rail.query != null && rail.movies.isNotEmpty()) {
            pages.remember(rail.movies)
        }
        loadMore()
    }

    fun loadMore(force: Boolean = false) {
        val query = rail.query ?: return
        if (end) return
        if (failed && !force) return
        if (loading) {
            queued = true
            return
        }
        failed = false
        queued = false
        loading = true
        viewModelScope.launch {
            try {
                var skips = 0
                while (true) {
                    var stop = true
                    val page = pages.page + 1
                    container.getShelfMovies.paged(query, page)
                        .onSuccess { batch ->
                            val take = pages.advance(batch.movies, hasMore = batch.hasMore, paged = true)
                            when {
                                take.again && skips + 1 >= ShelfOverlapSkips -> {
                                    skips++
                                    end = true
                                }
                                take.again -> {
                                    skips++
                                    stop = false
                                }
                                else -> {
                                    movies = movies + take.fresh
                                    end = take.end
                                }
                            }
                        }
                        .onFailure {
                            failed = true
                            stop = true
                        }
                    if (stop) break
                }
            } finally {
                loading = false
                if (queued && !end && !failed) {
                    queued = false
                    loadMore()
                } else {
                    queued = false
                }
            }
        }
    }

    class Factory(
        private val container: AppContainer,
        private val rail: TvRail,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TvShelfListViewModel(container, rail) as T
    }
}
