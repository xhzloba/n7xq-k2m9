package com.example.movie.presentation.kinopoisk

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.movie.R
import com.example.movie.presentation.common.drawTopScrim
import com.example.movie.presentation.common.headerFillOn
import com.example.movie.presentation.common.headerThemeBrush
import com.example.movie.presentation.common.topScrimFade
import com.example.movie.presentation.common.topScrimHeight
import com.example.movie.presentation.common.topScrimPalette
import com.example.movie.presentation.home.components.LogoAspect
import com.example.movie.presentation.home.components.LogoHeight
import com.example.movie.data.kinopoisk.KinopoiskCatalog
import com.example.movie.data.kinopoisk.KpShelf
import com.example.movie.data.kinopoisk.kpCard
import com.example.movie.data.kinopoisk.prefetchKpCards
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.hidesBar
import com.example.movie.presentation.common.HideBarOnScroll
import com.example.movie.presentation.common.scrollToTopOn
import com.example.movie.presentation.home.CatalogGap
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.BrandWordmark
import com.example.movie.presentation.home.components.HomeContentInset
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.RecommendationRail
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.ui.theme.PlayBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val KpRailPreview = 15

@Composable
private fun KpUpdatedMark(label: String) {
    val description = stringResource(R.string.kp_updated, label)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Icon(
            imageVector = ShellIcons.history,
            contentDescription = null,
            tint = CinemaMuted,
            modifier = Modifier.size(11.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = label,
            color = CinemaMuted,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
internal fun KinopoiskScreen(
    scrollTop: Int,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onOpenList: (String) -> Unit,
    onHideBar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var shelves by remember { mutableStateOf<List<KpShelf>?>(null) }
    LaunchedEffect(Unit) {
        shelves = withContext(Dispatchers.IO) { KinopoiskCatalog.load(context) }
    }
    val density = LocalDensity.current
    var brandHeight by remember { mutableIntStateOf(0) }
    val brandReserve = if (brandHeight == 0) 40.dp else with(density) { brandHeight.toDp() }
    val topInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val listState = rememberLazyListState()
    listState.scrollToTopOn(scrollTop)
    val scrimDistance = with(density) { 28.dp.toPx() }
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    HideBarOnScroll(
        enabled = settings.hidesBar(),
        listState = listState,
        onHideBar = onHideBar,
    )
    val loaded = shelves
    val container = LocalAppContainer.current
    LaunchedEffect(loaded) {
        val shelves = loaded ?: return@LaunchedEffect
        prefetchKpCards(shelves.flatMap { it.movies.take(KpRailPreview) }) { id ->
            val detail = container.getMovieDetail(id, background = true).getOrNull()
            if (detail != null && detail.id != id) container.favorites.note(detail.id, id)
            detail
        }
    }
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = topInset + BrandTopInset + brandReserve + 28.dp,
                bottom = 96.dp,
            ),
        ) {
            if (loaded == null) {
                items(4, key = { "kp-wait-$it" }) { index ->
                    if (index > 0) Spacer(Modifier.height(CatalogGap))
                    RecommendationRail(
                        movies = emptyList(),
                        onMovieClick = {},
                        onMoviePeek = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(loaded, key = { it.id }) { shelf ->
                    if (shelf.id != loaded.first().id) Spacer(Modifier.height(CatalogGap))
                    val preview = shelf.movies.take(KpRailPreview).map { it.kpCard() }
                    RecommendationRail(
                        movies = preview,
                        onMovieClick = onMovieClick,
                        onMoviePeek = onMoviePeek,
                        title = shelf.title,
                        onOpen = { onOpenList(shelf.id) },
                        trailing = shelf.updated?.let { label ->
                            { KpUpdatedMark(label) }
                        },
                        overlay = { movie ->
                            val rank = shelf.ranks[movie.id] ?: return@RecommendationRail
                            KpRankOverlay(rank)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        val palette = topScrimPalette()
        val fill = headerFillOn()
        val plate = headerThemeBrush()
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topScrimHeight(topInset))
                .topScrimFade(
                    if (listState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        (listState.firstVisibleItemScrollOffset / scrimDistance).coerceIn(0f, 1f)
                    },
                )
                .drawBehind {
                    drawTopScrim(fill, plate, null, palette) {
                        val y = topInset.toPx() + BrandTopInset.toPx() + 20.dp.toPx()
                        val radius = 140.dp.toPx()
                        val logoWidth = LogoHeight.toPx() * LogoAspect
                        val logoX = (HomeContentInset - BrandLogoBleed).toPx() + logoWidth / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0f to palette.poolCore,
                                    0.45f to palette.poolMid,
                                    1f to Color.Transparent,
                                ),
                                center = Offset(logoX, y),
                                radius = radius,
                            ),
                            radius = radius,
                            center = Offset(logoX, y),
                        )
                    }
                },
        )
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = BrandTopInset)
                .height(40.dp)
                .padding(start = HomeContentInset - BrandLogoBleed)
                .onSizeChanged { brandHeight = it.height },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandWordmark(play = PlayBlue)
        }
    }
}
