package com.example.movie.tvui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.movie.R
import com.example.movie.di.AppContainer
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.GenreRails
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.home.HomeRails
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class TvRail(
    val id: String,
    @StringRes val titleRes: Int,
    val movies: List<Movie> = emptyList(),
    val ready: Boolean = false,
    val failed: Boolean = false,
)

internal class TvHomeViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val specs = buildList {
        add(Spec("popular", R.string.catalog_popular, CatalogQuery("popular")))
        add(Spec("watching", R.string.section_watching, CatalogQuery(sort = "watching")))
        add(Spec("recommendations", R.string.section_recommendations, recommendations = true))
        HomeRails.Genres.forEach { entry ->
            val genre = GenreRails.All.first { it.id == entry.id }
            add(
                Spec(
                    id = entry.id,
                    titleRes = entry.titleRes,
                    query = CatalogQuery(sort = "new", genre = genre.genre),
                    background = true,
                ),
            )
        }
    }

    private val _rails = MutableStateFlow(fresh())
    val rails: StateFlow<List<TvRail>> = _rails.asStateFlow()

    private var job: Job? = null

    init {
        reload()
    }

    fun reload() {
        job?.cancel()
        _rails.value = fresh()
        job = viewModelScope.launch {
            specs.forEach { spec ->
                launch {
                    val result = fetch(spec)
                    if (!isActive) return@launch
                    publish(spec.id, result)
                }
            }
        }
    }

    private fun fresh(): List<TvRail> = specs.map { TvRail(id = it.id, titleRes = it.titleRes) }

    private suspend fun fetch(spec: Spec): Result<List<Movie>> {
        val first = load(spec)
        if (first.isSuccess) return first
        delay(400)
        return load(spec)
    }

    private suspend fun load(spec: Spec): Result<List<Movie>> {
        val result = if (spec.recommendations) {
            container.getRecommendations()
        } else {
            container.getShelfMovies(spec.query!!, limit = RailSize, background = spec.background)
        }
        return result.map { movies -> movies.distinctBy { it.id }.take(RailSize) }
    }

    private fun publish(id: String, result: Result<List<Movie>>) {
        _rails.update { list ->
            list.map { rail ->
                if (rail.id != id) {
                    rail
                } else if (result.isSuccess) {
                    rail.copy(movies = result.getOrThrow(), ready = true, failed = false)
                } else {
                    rail.copy(ready = true, failed = true)
                }
            }.filter { rail ->
                rail.id in Primary || !rail.ready || rail.movies.isNotEmpty()
            }
        }
    }

    private data class Spec(
        val id: String,
        @StringRes val titleRes: Int,
        val query: CatalogQuery? = null,
        val recommendations: Boolean = false,
        val background: Boolean = false,
    )

    companion object {
        const val RailSize = 15
        val Primary = setOf("popular", "watching", "recommendations")

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return TvHomeViewModel(container) as T
                }
            }
    }
}
