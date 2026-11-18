package com.example.movie.data.cast

import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.Movie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Что сейчас играет на плеере телевизора. Браузер сюда не попадает. */
data class CastProgram(
    val movie: Movie,
    val watchId: Int?,
    val seasons: List<FranchiseSeason>,
    val season: Int,
    val episode: Int,
)

data class TvCastUi(
    val message: String = "",
    val busy: Boolean = false,
    val devices: List<DlnaDevice> = emptyList(),
    val browserUrl: String? = null,
    val streamUrl: String? = null,
    val playingOn: String? = null,
    val hls: Boolean = false,
    val running: Boolean = false,
    val searching: Boolean = false,
    val qualities: List<Int> = emptyList(),
    val audios: List<String> = emptyList(),
    val quality: Int? = null,
    val audio: Int = 0,
    val player: CastProgram? = null,
)

object TvCastHub {
    private val mutable = MutableStateFlow(TvCastUi())
    val state: StateFlow<TvCastUi> = mutable.asStateFlow()

    @Volatile
    internal var armed: CastProgram? = null

    fun arm(program: CastProgram) {
        armed = program
    }

    fun sync(program: CastProgram) {
        armed = program
        update { state ->
            if (state.player == null) state else state.copy(player = program)
        }
    }

    internal fun update(block: (TvCastUi) -> TvCastUi) {
        mutable.update(block)
    }
}
