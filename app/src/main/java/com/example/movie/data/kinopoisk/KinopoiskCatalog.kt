package com.example.movie.data.kinopoisk

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal data class KpRank(
    val position: Int,
    val diff: Int?,
    val up: Boolean?,
    val boxOffice: String? = null,
)

internal data class KpShelf(
    val id: String,
    val title: String,
    val updated: String?,
    val movies: List<Movie>,
    val ranks: Map<String, KpRank>,
)

private val Files = listOf(
    "kinopoisk/popular-films.json",
    "kinopoisk/popular-series.json",
    "kinopoisk/top500.json",
    "kinopoisk/series-top250.json",
    "kinopoisk/box-usa-all-time.json",
)

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

private val kpCards = mutableStateMapOf<String, Movie>()

internal fun Movie.kpCard(): Movie = kpCards[id] ?: this

internal fun Movie.kpApply(detail: MovieDetail): Movie {
    val next = copy(
        logoUrl = detail.logoUrl ?: logoUrl,
        backdropUrl = detail.backdrops.firstOrNull() ?: backdropUrl,
        overview = detail.overview.ifBlank { overview },
        genres = detail.genres.joinToString().ifBlank { genres },
        country = detail.country.ifBlank { country },
        director = detail.director.ifBlank { director },
        ratingKp = ratingKp ?: detail.ratingKp,
        ratingImdb = detail.ratingImdb ?: ratingImdb,
        age = detail.age ?: age,
        year = detail.year ?: year,
    )
    kpCards[id] = next
    return next
}

internal suspend fun prefetchKpCards(
    movies: List<Movie>,
    load: suspend (String) -> MovieDetail?,
) {
    val pending = movies.distinctBy { it.id }.filter { kpCards[it.id]?.logoUrl.isNullOrBlank() && it.logoUrl.isNullOrBlank() }
    if (pending.isEmpty()) return
    val gate = Semaphore(3)
    coroutineScope {
        pending.forEach { movie ->
            launch {
                gate.withPermit {
                    if (!kpCards[movie.id]?.logoUrl.isNullOrBlank()) return@withPermit
                    val detail = load(movie.id) ?: return@withPermit
                    movie.kpApply(detail)
                }
            }
        }
    }
}

internal object KinopoiskCatalog {
    private val lock = Any()
    private var cached: List<KpShelf>? = null

    fun peek(id: String): KpShelf? = synchronized(lock) { cached?.firstOrNull { it.id == id } }

    fun load(context: Context): List<KpShelf> = synchronized(lock) {
        cached?.let { return it }
        val shelves = Files.map { path ->
            context.assets.open(path).bufferedReader().use { it.readText() }
                .let { raw -> json.decodeFromString(KpFile.serializer(), raw) }
                .toShelf()
        }
        cached = shelves
        shelves
    }
}

@Serializable
private data class KpFile(
    val category: String,
    @SerialName("category_title") val categoryTitle: String,
    @SerialName("parsed_at") val parsedAt: String = "",
    val items: List<KpItem> = emptyList(),
)

@Serializable
private data class KpItem(
    @SerialName("kp_id") val kpId: Long,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val poster: String? = null,
    val position: Int? = null,
    val diff: Int? = null,
    @SerialName("diff_kind") val diffKind: String? = null,
    @SerialName("box_office") val boxOffice: String? = null,
)

private fun KpFile.toShelf(): KpShelf {
    val series = category == "popular-series" || category == "series-top250"
    val seen = HashSet<String>()
    val movies = ArrayList<Movie>(items.size)
    val ranks = HashMap<String, KpRank>(items.size)
    for (item in items) {
        val title = item.title.trim()
        if (title.isEmpty() || item.kpId <= 0L) continue
        val id = item.kpId.toString()
        if (!seen.add(id)) continue
        movies += item.toMovie(id, title, series)
        val place = item.position?.takeIf { it > 0 }
        val gross = item.boxOffice?.trim()?.takeIf { it.isNotEmpty() }
        if (place == null && gross == null) continue
        ranks[id] = KpRank(
            position = place ?: 0,
            diff = item.diff?.takeIf { it > 0 },
            up = when (item.diffKind) {
                "positive" -> true
                "negative" -> false
                else -> null
            },
            boxOffice = gross,
        )
    }
    return KpShelf(
        id = category,
        title = categoryTitle,
        updated = kpUpdatedLabel(parsedAt),
        movies = movies,
        ranks = ranks,
    )
}

private val KpMonths = arrayOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
)

internal fun kpUpdatedLabel(raw: String): String? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    val date = runCatching { OffsetDateTime.parse(text).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(text.take(10)) }.getOrNull()
        ?: return null
    return "${date.dayOfMonth} ${KpMonths[date.monthValue - 1]} ${date.year}"
}

private fun KpItem.toMovie(id: String, title: String, series: Boolean): Movie = Movie(
    id = id,
    title = title,
    originalTitle = originalTitle?.trim().orEmpty(),
    year = year?.takeIf { it > 0 },
    overview = "",
    genres = "",
    country = "",
    director = "",
    posterUrl = poster?.takeIf { it.isNotBlank() },
    backdropUrl = null,
    logoUrl = null,
    ratingKp = rating?.takeIf { it > 0.0 }?.toFloat(),
    ratingImdb = null,
    age = null,
    kind = if (series) ContentKind.Series else ContentKind.Movie,
    camrip = false,
    type = if (series) "serial" else "movie",
)
