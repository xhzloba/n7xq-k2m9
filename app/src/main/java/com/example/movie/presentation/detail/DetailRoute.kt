package com.example.movie.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.isKinopoiskId
import com.example.movie.domain.model.toSaved

@Composable
fun DetailRoute(
    movieId: String,
    onOpenMovie: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: DetailViewModel = viewModel(key = movieId) {
        DetailViewModel(
            movieId = movieId,
            getMovieDetail = container.getMovieDetail,
            watchCheck = container.watchCheck,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail = state.detail
    val openedFromKp = movieId.isKinopoiskId()
    val vokinoKp = detail?.franchise?.kinopoiskId?.takeIf { it > 0 }?.toString()
    LaunchedEffect(movieId, detail?.id, detail?.posterUrl, if (openedFromKp) null else vokinoKp) {
        val card = detail ?: return@LaunchedEffect
        if (openedFromKp) {
            if (card.id != movieId) container.viewHistory.note(card.id, movieId)
            val shown = if (card.id == movieId) card.toSaved()
                else DetailSeed.get(movieId)?.toSaved() ?: return@LaunchedEffect
            container.viewHistory.remember(shown)
        } else if (card.id == movieId) {
            if (vokinoKp != null) container.viewHistory.note(card.id, vokinoKp)
            val saved = if (vokinoKp == null) card.toSaved() else card.toSaved().copy(kpId = vokinoKp)
            container.viewHistory.remember(saved)
        }
    }
    DetailScreen(
        state = state,
        onRetry = viewModel::retry,
        onResolveWatch = viewModel::resolveWatch,
        onOpenMovie = onOpenMovie,
        onMoviePeek = onMoviePeek,
        onBack = onBack,
    )
}
