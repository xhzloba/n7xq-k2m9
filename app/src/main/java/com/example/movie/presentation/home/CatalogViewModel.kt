package com.example.movie.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.R
import com.example.movie.domain.model.CatalogQuery
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.Movie
import com.example.movie.domain.usecase.GetShelfMoviesUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class GenreSort(val sort: String, @StringRes val title: Int) {
    Default("", R.string.catalog_sort_default),
    Added("new", R.string.catalog_sort_added),
    Updates("updatings", R.string.catalog_sort_updates),
    Popular("popular", R.string.catalog_sort_popular),
    Rating("rating", R.string.catalog_sort_rating),
}

internal enum class CatalogSort(
    val sort: String,
    @StringRes val title: Int,
    val country: String? = null,
) {
    Watching("watching", R.string.section_watching),
    Popular("popular", R.string.catalog_popular),
    Fresh("new", R.string.catalog_new),
    Updates("updatings", R.string.catalog_updates),
    Russian("popular", R.string.catalog_russian, country = "Россия"),
    Best("rating", R.string.section_best),
    ;

    /** Ключ полного списка. Страна отдельно: у «Популярное» и «Популярные российские» один sort. */
    fun listKey(type: String): String =
        if (country.isNullOrBlank()) "$type:$sort" else "$type:$sort:c:$country"

    companion object {
        fun forType(type: String): List<CatalogSort> =
            entries.filter { entry ->
                when (entry) {
                    Watching -> type == "movie"
                    Russian -> type == "movie" || type == "serial"
                    else -> true
                }
            }
    }
}

internal data class OpenedCatalogShelf(
    val type: String,
    val sort: String,
    val country: String?,
    @StringRes val title: Int,
)

/** `movie:popular` или `movie:popular:c:Россия`. Жанровый ключ `type:g:…` сюда не входит. */
internal fun openedCatalogShelf(key: String): OpenedCatalogShelf? {
    val type = key.substringBefore(':')
    if (type.isEmpty() || !key.contains(':')) return null
    val rest = key.substringAfter(':')
    if (rest.startsWith("g:")) return null
    val country = if (CountryMarker in rest) {
        rest.substringAfter(CountryMarker).takeIf { it.isNotBlank() }
    } else {
        null
    }
    val sort = if (country == null) rest else rest.substringBefore(CountryMarker)
    val match = CatalogSort.entries.firstOrNull { it.sort == sort && it.country == country } ?: return null
    return OpenedCatalogShelf(type, match.sort, match.country, match.title)
}

private const val CountryMarker = ":c:"

internal data class CatalogRail(
    val sort: CatalogSort,
    val movies: List<Movie> = emptyList(),
    val ready: Boolean = false,
)

internal data class CatalogUiState(
    val shelves: List<CatalogRail> = emptyList(),
    val refreshing: Boolean = false,
)

internal class CatalogViewModel(
    private val type: String,
    private val getShelfMovies: GetShelfMoviesUseCase,
) : ViewModel() {
    private val sorts = CatalogSort.forType(type)
    private val _state = MutableStateFlow(CatalogUiState(sorts.map { CatalogRail(it) }))
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var started = false

    fun ensureLoaded() {
        if (started) return
        started = true
        load(keep = false)
    }

    fun retry() {
        started = true
        _state.value = CatalogUiState(sorts.map { CatalogRail(it) })
        load(keep = false)
    }

    fun refresh() {
        if (_state.value.refreshing) return
        load(keep = true)
    }

    private fun load(keep: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (keep) _state.update { it.copy(refreshing = true) }
            try {
                // Пачками и в фоне: открытая карточка и полный список не ждут все полки и не ловят 429.
                coroutineScope {
                    repeat(CatalogLanes) { lane ->
                        launch {
                            var index = lane
                            while (index < sorts.size) {
                                fetch(sorts[index], keep)
                                index += CatalogLanes
                            }
                        }
                    }
                }
            } finally {
                if (keep) _state.update { it.copy(refreshing = false) }
            }
        }
    }

    private suspend fun fetch(sort: CatalogSort, keep: Boolean) {
        val query = CatalogQuery(sort = sort.sort, type = type, country = sort.country)
        val movies = ArrayList<Movie>(CatalogRailSize)
        val seen = HashSet<String>()
        var failed = false
        var page = 1
        while (movies.size < CatalogRailSize && page <= CatalogRailPages) {
            val batch = getShelfMovies.paged(query, page = page, background = true).getOrNull()
            if (batch == null) {
                failed = true
                break
            }
            var added = 0
            for (movie in batch.movies) {
                if (!seen.add(movie.id)) continue
                movies.add(movie)
                added++
                if (movies.size == CatalogRailSize) break
            }
            if (movies.size == CatalogRailSize || !batch.hasMore || batch.movies.isEmpty() || added == 0) break
            page++
        }
        _state.update { current ->
            current.copy(
                shelves = current.shelves.map { rail ->
                    if (rail.sort != sort) {
                        rail
                    } else {
                        val next = when {
                            movies.isNotEmpty() -> movies
                            failed && keep -> rail.movies
                            else -> emptyList()
                        }
                        rail.copy(movies = next, ready = true)
                    }
                },
            )
        }
    }
}

internal class CatalogListViewModel(
    private val query: CatalogQuery,
    private val getShelfMovies: GetShelfMoviesUseCase,
) : ViewModel() {
    private val pages = ShelfPages()
    private var busy = false
    private var queued = false
    private val _state = MutableStateFlow(SectionUiState())
    val state: StateFlow<SectionUiState> = _state.asStateFlow()

    init {
        loadMore()
    }

    fun retry() {
        if (_state.value.movies.isEmpty()) {
            pages.reset()
            _state.value = SectionUiState()
        }
        loadMore(force = true)
    }

    fun loadMore(force: Boolean = false) {
        val current = _state.value
        if (current.end) return
        if (!force && current.error != null) return
        if (busy) {
            queued = true
            return
        }
        queued = false
        busy = true
        val first = current.movies.isEmpty()
        _state.value = current.copy(loading = first, loadingMore = !first, error = null)
        viewModelScope.launch {
            try {
                var skips = 0
                while (true) {
                    var stop = true
                    getShelfMovies.paged(query, page = pages.page + 1)
                        .onSuccess { batch ->
                            val take = pages.advance(batch.movies, hasMore = batch.hasMore, paged = true)
                            when {
                                take.again && skips + 1 >= ShelfOverlapSkips -> {
                                    skips++
                                    _state.value = _state.value.copy(
                                        loading = false,
                                        loadingMore = false,
                                        end = true,
                                        error = null,
                                    )
                                }
                                take.again -> {
                                    skips++
                                    stop = false
                                    _state.value = _state.value.copy(
                                        loading = false,
                                        loadingMore = true,
                                        end = false,
                                        error = null,
                                    )
                                }
                                else -> _state.value = _state.value.copy(
                                    movies = _state.value.movies + take.fresh,
                                    loading = false,
                                    loadingMore = false,
                                    end = take.end,
                                    error = null,
                                )
                            }
                        }
                        .onFailure { error ->
                            _state.value = _state.value.copy(
                                loading = false,
                                loadingMore = false,
                                error = error as? DataError ?: DataError.Unknown(),
                            )
                        }
                    if (stop) break
                }
            } finally {
                busy = false
                if (queued && !_state.value.end && _state.value.error == null) {
                    queued = false
                    loadMore()
                } else {
                    queued = false
                }
            }
        }
    }
}

/** Сколько полок каталога качаем сразу. Дальше — следующая пачка, в фоне. */
private const val CatalogLanes = 4

/** В слайдере 15. У «Сейчас смотрят» страница короче, добираем следующими. */
private const val CatalogRailSize = 15

private const val CatalogRailPages = 4
