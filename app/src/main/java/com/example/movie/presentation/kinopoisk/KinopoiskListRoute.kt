package com.example.movie.presentation.kinopoisk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.movie.data.kinopoisk.KinopoiskCatalog
import com.example.movie.data.kinopoisk.KpShelf
import com.example.movie.data.kinopoisk.kpCard
import com.example.movie.data.kinopoisk.prefetchKpCards
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.home.SectionPage
import com.example.movie.presentation.home.SectionUiState
import kotlinx.coroutines.Dispatchers
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
    var shelf by remember(id) { mutableStateOf(KinopoiskCatalog.peek(id)) }
    LaunchedEffect(id) {
        if (shelf == null) {
            shelf = withContext(Dispatchers.IO) {
                KinopoiskCatalog.load(context).firstOrNull { it.id == id }
            }
        }
    }
    val current = shelf
    val container = LocalAppContainer.current
    val movies = current?.movies.orEmpty()
    LaunchedEffect(current?.id) {
        if (movies.isEmpty()) return@LaunchedEffect
        prefetchKpCards(movies.take(24)) { id ->
            container.getMovieDetail(id, background = true).getOrNull()
        }
    }
    SectionPage(
        title = current?.title.orEmpty(),
        state = SectionUiState(
            movies = movies.map { it.kpCard() },
            loading = current == null,
            end = true,
        ),
        onBack = onBack,
        onLoadMore = {},
        onRetry = {},
        onMovieClick = onMovieClick,
        onMoviePeek = onMoviePeek,
        onHideBar = onHideBar,
        overlay = { movie ->
            val rank = current?.ranks?.get(movie.id) ?: return@SectionPage
            KpRankOverlay(rank)
        },
        modifier = modifier,
    )
}
