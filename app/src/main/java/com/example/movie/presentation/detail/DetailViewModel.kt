package com.example.movie.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie.domain.model.DataError
import com.example.movie.domain.model.MovieDetail
import com.example.movie.domain.model.WatchCheck
import com.example.movie.domain.usecase.GetMovieDetailUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DetailViewModel(
    private val movieId: String,
    private val getMovieDetail: GetMovieDetailUseCase,
    private val watchCheck: suspend (String, String?, Int?) -> WatchCheck,
) : ViewModel() {
    private val _state = MutableStateFlow(
        DetailSeed.get(movieId)?.toPreviewDetail()?.let { preview ->
            DetailUiState(detail = preview, phase = DetailPhase.Loading)
        } ?: DetailUiState(),
    )
    val state: StateFlow<DetailUiState> = _state.asStateFlow()
    private var watchJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun resolveWatch() {
        val detail = _state.value.detail ?: return
        resolveWatch(detail)
    }

    private fun load() {
        watchJob?.cancel()
        viewModelScope.launch {
            if (_state.value.detail == null) {
                _state.value = DetailUiState(phase = DetailPhase.Loading)
            }
            getMovieDetail(movieId)
                .onSuccess { loaded ->
                    val detail = loaded.withKinopoiskSeed(movieId)
                    _state.value = DetailUiState(detail = detail, phase = DetailPhase.Ready)
                    resolveWatch(detail)
                }
                .onFailure { error ->
                    if (_state.value.detail == null) {
                        _state.value = DetailUiState(phase = DetailPhase.Failed(error.asDataError()))
                    }
                }
        }
    }

    private fun resolveWatch(detail: MovieDetail) {
        watchJob?.cancel()
        watchJob = viewModelScope.launch {
            _state.value = _state.value.copy(franchiseReady = false, unavailable = false)
            // Таймаут, обрыв сети и неответивший эмбед — это Unknown, не «плеера нет».
            // Лоадер гас, качество не появлялось, второй тап по «Смотреть» проходил.
            // Один повтор, пока лоадер ещё крутится. Второй Unknown оставляем как есть.
            val lookupId = movieId.takeIf(::isKinopoiskId) ?: detail.id
            val first = watchCheck(lookupId, detail.title, detail.year)
            val check = if (first is WatchCheck.Unknown) {
                watchCheck(lookupId, detail.title, detail.year)
            } else {
                first
            }
            val current = _state.value.detail
            if (current?.id != detail.id) return@launch
            val franchise = (check as? WatchCheck.Ready)?.franchise
            _state.value = _state.value.copy(
                detail = current.copy(franchise = franchise),
                franchiseReady = true,
                unavailable = check is WatchCheck.Missing || (franchise != null && !franchise.playable),
            )
        }
    }
}

private fun Throwable.asDataError(): DataError = this as? DataError ?: DataError.Unknown()

private fun isKinopoiskId(id: String): Boolean =
    id.length in 1..10 && id.all(Char::isDigit) && id.toIntOrNull()?.let { it > 0 } == true

/**
 * Вокино на неизвестный id Кинопоиска отвечает пустой карточкой.
 * Тогда остаётся название и рейтинг с полки. Если фильм есть, рейтинг
 * парсинга всё равно не затирается. IMDb берётся из Вокино.
 */
private fun MovieDetail.withKinopoiskSeed(openedId: String): MovieDetail {
    if (!isKinopoiskId(openedId)) return this
    val seed = DetailSeed.get(openedId)
    if (title.isBlank() && seed != null && seed.title.isNotBlank()) {
        val missing = id.isBlank() || id == openedId
        if (missing) return seed.toPreviewDetail()
        return copy(
            title = seed.title,
            ratingKp = seed.ratingKp ?: ratingKp,
            posterUrl = posterUrl ?: seed.posterUrl,
            year = year ?: seed.year,
        )
    }
    val parsed = seed?.ratingKp ?: return this
    return if (ratingKp == parsed) this else copy(ratingKp = parsed)
}
