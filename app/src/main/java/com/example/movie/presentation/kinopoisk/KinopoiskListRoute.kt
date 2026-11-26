package com.example.movie.presentation.kinopoisk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.data.kinopoisk.KinopoiskCatalog
import com.example.movie.data.kinopoisk.kpCard
import com.example.movie.data.kinopoisk.prefetchKpCards
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.home.SectionPage
import com.example.movie.presentation.home.SectionUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun KinopoiskListRoute(
    id: String,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onHideBar: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val revision by KinopoiskCatalog.revision.collectAsStateWithLifecycle()
    var slice by remember(id) { mutableStateOf(KinopoiskCatalog.peekSlice(id)) }
    var loadingMore by remember(id) { mutableStateOf(false) }
    val container = LocalAppContainer.current
    LaunchedEffect(id, revision) {
        val known = if (revision == 0) slice else null
        val first = known ?: withContext(Dispatchers.IO) {
            KinopoiskCatalog.page(context, id, 0)
        }
        slice = first
        loadingMore = false
        if (first.movies.isEmpty()) return@LaunchedEffect
        prefetchKpCards(first.movies) { movieId ->
            val detail = container.getMovieDetail(movieId, background = true).getOrNull()
            if (detail != null && detail.id != movieId) {
                container.favorites.note(detail.id, movieId)
                container.watched.note(detail.id, movieId)
            }
            detail
        }
    }
    val current = slice
    SectionPage(
        title = current?.title.orEmpty(),
        state = SectionUiState(
            movies = current?.movies.orEmpty().map { it.kpCard() },
            loading = current == null,
            loadingMore = loadingMore,
            end = current?.end == true,
        ),
        onBack = onBack,
        onLoadMore = {
            val shown = current ?: return@SectionPage
            if (shown.end || loadingMore) return@SectionPage
            loadingMore = true
            scope.launch {
                val next = withContext(Dispatchers.IO) {
                    KinopoiskCatalog.page(context, id, shown.nextOffset)
                }
                slice = shown + next
                loadingMore = false
                prefetchKpCards(next.movies) { movieId ->
                    val detail = container.getMovieDetail(movieId, background = true).getOrNull()
                    if (detail != null && detail.id != movieId) {
                        container.favorites.note(detail.id, movieId)
                        container.watched.note(detail.id, movieId)
                    }
                    detail
                }
            }
        },
        onRetry = {},
        onMovieClick = onMovieClick,
        onMoviePeek = onMoviePeek,
        onHideBar = onHideBar,
        note = if (current?.id == "box-usa-all-time") {
            stringResource(R.string.kp_box_usa_about)
        } else {
            null
        },
        overlay = { movie ->
            val rank = current?.ranks?.get(movie.id) ?: return@SectionPage
            KpRankOverlay(rank)
        },
        modifier = modifier,
    )
}
