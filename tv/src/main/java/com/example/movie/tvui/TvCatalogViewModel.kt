package com.example.movie.tvui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.movie.R
import com.example.movie.di.AppContainer
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.home.CatalogSort
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class TvCategory(
    val id: String,
    @StringRes val label: Int,
)

internal class TvCatalogViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _type = MutableStateFlow(Kinds[0].id)
    val type: StateFlow<String> = _type.asStateFlow()

    private val _rails = MutableStateFlow<List<TvRail>>(emptyList())
    val rails: StateFlow<List<TvRail>> = _rails.asStateFlow()

    private var job: Job? = null
    private var loadedType: String? = null
    private val cache = HashMap<String, List<TvRail>>()

    fun show(type: String) {
        if (type == loadedType && _rails.value.isNotEmpty()) return
        _type.value = type
        loadedType = type
        val saved = cache[type]
        if (saved != null && saved.all { it.ready }) {
            _rails.value = saved
            return
        }
        reload()
    }

    fun reload() {
        job?.cancel()
        val type = _type.value
        cache.remove(type)
        val sorts = CatalogSort.forType(type)
        _rails.value = sorts.map { sort ->
            TvRail(
                id = "$type:${sort.name}",
                titleRes = sort.title,
                query = CatalogQuery(sort = sort.sort, type = type, country = sort.country),
            )
        }
        job = viewModelScope.launch {
            sorts.forEach { sort ->
                launch {
                    val loaded = load(type, sort)
                    if (!isActive) return@launch
                    publish("$type:${sort.name}", loaded)
                }
            }
        }
    }

    private suspend fun load(type: String, sort: CatalogSort): Result<List<Movie>> {
        val query = CatalogQuery(sort = sort.sort, type = type, country = sort.country)
        val movies = ArrayList<Movie>(RailSize)
        val seen = HashSet<String>()
        var failed = false
        var page = 1
        while (movies.size < RailSize && page <= Pages) {
            val batch = container.getShelfMovies.paged(query, page = page, background = true).getOrNull()
            if (batch == null) {
                failed = true
                break
            }
            var added = 0
            for (movie in batch.movies) {
                if (!seen.add(movie.id)) continue
                movies.add(movie)
                added++
                if (movies.size == RailSize) break
            }
            if (movies.size == RailSize || !batch.hasMore || batch.movies.isEmpty() || added == 0) break
            page++
        }
        return if (movies.isNotEmpty() || !failed) Result.success(movies) else Result.failure(IllegalStateException("catalog"))
    }

    private fun publish(id: String, result: Result<List<Movie>>) {
        _rails.update { list ->
            val next = list.map { rail ->
                if (rail.id != id) {
                    rail
                } else if (result.isSuccess) {
                    rail.copy(movies = result.getOrThrow(), ready = true, failed = false)
                } else {
                    rail.copy(ready = true, failed = true)
                }
            }.filter { rail -> !rail.ready || rail.failed || rail.movies.isNotEmpty() }
            if (next.isNotEmpty()) {
                next
            } else {
                listOf(
                    TvRail(
                        id = id,
                        titleRes = list.firstOrNull()?.titleRes ?: R.string.catalog_popular,
                        ready = true,
                        failed = true,
                    ),
                )
            }
        }
        val published = _rails.value
        if (_type.value == id.substringBefore(':') && published.all { it.ready }) {
            cache[_type.value] = published
        }
    }

    companion object {
        const val RailSize = 15
        private const val Pages = 4

        val Kinds = listOf(
            TvCategory("movie", R.string.catalog_movies),
            TvCategory("serial", R.string.catalog_series),
            TvCategory("multfilm", R.string.catalog_cartoons),
            TvCategory("multserial", R.string.catalog_cartoon_series),
            TvCategory("anime", R.string.catalog_anime),
        )

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return TvCatalogViewModel(container) as T
                }
            }
    }
}
