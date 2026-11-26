package com.example.movie.data.kinopoisk

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail
import java.io.File
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class KpRank(
    val position: Int,
    val diff: Int?,
    val up: Boolean?,
    val boxOffice: String? = null,
)

internal const val KpPageSize = 15

internal data class KpShelf(
    val id: String,
    val title: String,
    val updated: String?,
    val movies: List<Movie>,
    val ranks: Map<String, KpRank>,
    val nextOffset: Int,
    val end: Boolean,
)

/** Уже прочитанный кусок списка. `nextOffset` — с какого места читать следующую страницу. */
internal data class KpSlice(
    val id: String,
    val title: String,
    val updated: String?,
    val movies: List<Movie>,
    val ranks: Map<String, KpRank>,
    val nextOffset: Int,
    val end: Boolean,
) {
    operator fun plus(next: KpSlice): KpSlice {
        if (next.movies.isEmpty()) return copy(nextOffset = next.nextOffset, end = next.end)
        val seen = HashSet<String>(movies.size + next.movies.size)
        movies.mapTo(seen) { it.id }
        val fresh = next.movies.filter { seen.add(it.id) }
        return copy(
            movies = movies + fresh,
            ranks = ranks + next.ranks,
            nextOffset = next.nextOffset,
            end = next.end,
        )
    }
}

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

private const val CatalogRepo = "xhzloba/n7xq-k2m9"

internal fun kpCatalogUrl(path: String): String =
    "https://raw.githubusercontent.com/$CatalogRepo/main/app/src/main/assets/$path"

internal fun kpRemoteBody(fileName: String, raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val file = runCatching { json.decodeFromString(KpFile.serializer(), trimmed) }.getOrNull() ?: return null
    val expected = fileName.removeSuffix(".json")
    if (file.category != expected || file.items.isEmpty()) return null
    return trimmed
}

internal object KinopoiskCatalog {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()
    private val files = Mutex()
    private var ongoing: Deferred<List<KpShelf>>? = null
    @Volatile private var cached: List<KpShelf>? = null
    private val revisions = MutableStateFlow(0)
    val revision: StateFlow<Int> = revisions
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun peek(id: String): KpShelf? = cached?.firstOrNull { it.id == id }

    fun peekSlice(id: String): KpSlice? = peek(id)?.toSlice()

    fun snapshot(): List<KpShelf>? = cached

    /** Одна страница списка. На полке это первые [KpPageSize], дальше — при прокрутке полного списка. */
    suspend fun page(context: Context, id: String, offset: Int): KpSlice {
        val path = Files.firstOrNull { it.endsWith("/$id.json") } ?: return KpSlice(id, "", null, emptyList(), emptyMap(), offset, true)
        val app = context.applicationContext
        val text = withContext(Dispatchers.IO) {
            files.withLock { textFor(app, path) }
        }
        return kpSlice(text, offset, KpPageSize)
    }

    suspend fun ensure(context: Context): List<KpShelf> {
        cached?.let { return it }
        val app = context.applicationContext
        return withContext(Dispatchers.IO) {
            files.withLock { cached ?: readLocal(app).also { cached = it } }
        }
    }

    suspend fun refresh(context: Context): List<KpShelf> {
        val app = context.applicationContext
        val job = gate.withLock {
            val current = ongoing
            if (current != null && !current.isCompleted) {
                current
            } else {
                val created = scope.async { pull(app) }
                ongoing = created
                created.invokeOnCompletion {
                    scope.launch { gate.withLock { if (ongoing === created) ongoing = null } }
                }
                created
            }
        }
        return job.await()
    }

    private suspend fun pull(context: Context): List<KpShelf> {
        val fetched = coroutineScope {
            Files.map { path ->
                async { path to download(path) }
            }.awaitAll()
        }
        return files.withLock {
            var changed = false
            for ((path, text) in fetched) {
                if (text != null && write(context, path, text)) changed = true
            }
            val current = cached
            if (!changed && current != null) return@withLock current
            readLocal(context).also {
                cached = it
                revisions.value = revisions.value + 1
            }
        }
    }

    private fun download(path: String): String? {
        val request = Request.Builder()
            .url(kpCatalogUrl(path))
            .header("User-Agent", "AmigoPlay")
            .header("Accept", "application/json")
            .build()
        return try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                kpRemoteBody(path.substringAfterLast('/'), response.body?.string().orEmpty())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }
}

private val kpTexts = HashMap<String, String>()

private fun cacheDir(context: Context): File =
    File(context.filesDir, "kinopoisk").apply { mkdirs() }

private fun readLocal(context: Context): List<KpShelf> =
    Files.map { path ->
        kpSlice(textFor(context, path), 0, KpPageSize).toShelf()
    }

private fun textFor(context: Context, path: String): String {
    kpTexts[path]?.let { return it }
    val name = path.substringAfterLast('/')
    val disk = File(cacheDir(context), name)
    val diskText = if (disk.isFile) runCatching { disk.readText() }.getOrNull()?.trim() else null
    val raw = if (diskText != null && kpCachedBody(name, diskText)) {
        diskText
    } else {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }
    kpTexts[path] = raw
    return raw
}

private fun write(context: Context, path: String, text: String): Boolean {
    val name = path.substringAfterLast('/')
    val body = kpRemoteBody(name, text) ?: return false
    val dir = cacheDir(context)
    val dest = File(dir, name)
    if (dest.isFile && runCatching { dest.readText() }.getOrNull()?.trim() == body) return false
    kpTexts.remove(path)
    val tmp = File(dir, "$name.tmp")
    tmp.writeText(body)
    val backup = File(dir, "$name.bak")
    if (dest.exists() && !dest.renameTo(backup)) dest.delete()
    if (!tmp.renameTo(dest)) {
        if (backup.exists()) backup.renameTo(dest)
        tmp.delete()
        return false
    }
    backup.delete()
    return true
}

@Serializable
private data class KpFile(
    val category: String,
    @SerialName("category_title") val categoryTitle: String = "",
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

internal fun kpCachedBody(fileName: String, raw: String): Boolean {
    val trimmed = raw.trim()
    if (trimmed.length < 2 || trimmed.first() != '{' || trimmed.last() != '}') return false
    val expected = fileName.removeSuffix(".json")
    if (jsonString(trimmed, "category") != expected) return false
    val key = trimmed.indexOf("\"items\"")
    if (key < 0) return false
    val open = trimmed.indexOf('[', key + 7)
    if (open < 0) return false
    var index = open + 1
    while (index < trimmed.length && trimmed[index].isWhitespace()) index++
    return index < trimmed.length && trimmed[index] == '{'
}

private fun jsonString(raw: String, field: String): String? {
    val key = raw.indexOf("\"$field\"")
    if (key < 0) return null
    val colon = raw.indexOf(':', key + field.length + 2)
    if (colon < 0) return null
    val quote = raw.indexOf('"', colon + 1)
    if (quote < 0) return null
    val end = raw.indexOf('"', quote + 1)
    if (end < 0) return null
    return raw.substring(quote + 1, end)
}

/**
 * Страница из большого JSON. Разбираются только [limit] карточек, начиная с [offset].
 * Остальной массив не превращается в фильмы.
 */
internal fun kpSlice(text: String, offset: Int, limit: Int): KpSlice {
    val head = headOf(text)
    val (items, next, end) = takeItems(text, offset, limit)
    val series = head?.category == "popular-series" || head?.category == "series-top250"
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
    return KpSlice(
        id = head?.category.orEmpty(),
        title = head?.categoryTitle?.ifBlank { head.category }.orEmpty(),
        updated = head?.let { kpUpdatedLabel(it.parsedAt) },
        movies = movies,
        ranks = ranks,
        nextOffset = next,
        end = end,
    )
}

private fun headOf(text: String): KpFile? {
    val marker = text.indexOf("\"items\"")
    if (marker < 0) return null
    val head = text.substring(0, marker) + "\"items\":[]}"
    return runCatching { json.decodeFromString(KpFile.serializer(), head) }.getOrNull()
}

private fun takeItems(text: String, offset: Int, limit: Int): Triple<List<KpItem>, Int, Boolean> {
    val key = text.indexOf("\"items\"")
    if (key < 0 || limit <= 0) return Triple(emptyList(), offset, true)
    val open = text.indexOf('[', key + 7)
    if (open < 0) return Triple(emptyList(), offset, true)
    val raw = ArrayList<String>(limit)
    var index = open + 1
    var depth = 0
    var inString = false
    var escape = false
    var start = -1
    var seen = 0
    var end = true
    while (index < text.length) {
        val char = text[index]
        if (inString) {
            if (escape) {
                escape = false
            } else if (char == '\\') {
                escape = true
            } else if (char == '"') {
                inString = false
            }
            index++
            continue
        }
        when (char) {
            '"' -> inString = true
            '{' -> {
                if (depth == 0) start = index
                depth++
            }
            '}' -> {
                if (depth > 0) {
                    depth--
                    if (depth == 0 && start >= 0) {
                        if (seen >= offset && raw.size < limit) raw += text.substring(start, index + 1)
                        seen++
                        start = -1
                        if (raw.size == limit) {
                            end = !moreItem(text, index + 1)
                            index = text.length
                            continue
                        }
                    }
                }
            }
            ']' -> if (depth == 0) {
                end = true
                index = text.length
                continue
            }
        }
        index++
    }
    val items = raw.mapNotNull { runCatching { json.decodeFromString(KpItem.serializer(), it) }.getOrNull() }
    return Triple(items, seen, end)
}

private fun moreItem(text: String, from: Int): Boolean {
    var index = from
    var inString = false
    var escape = false
    while (index < text.length) {
        val char = text[index]
        if (inString) {
            if (escape) {
                escape = false
            } else if (char == '\\') {
                escape = true
            } else if (char == '"') {
                inString = false
            }
            index++
            continue
        }
        when (char) {
            '"' -> inString = true
            '{' -> return true
            ']' -> return false
        }
        index++
    }
    return false
}

private fun KpSlice.toShelf(): KpShelf = KpShelf(
    id = id,
    title = title,
    updated = updated,
    movies = movies,
    ranks = ranks,
    nextOffset = nextOffset,
    end = end,
)

private fun KpShelf.toSlice(): KpSlice = KpSlice(
    id = id,
    title = title,
    updated = updated,
    movies = movies,
    ranks = ranks,
    nextOffset = nextOffset,
    end = end,
)

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
