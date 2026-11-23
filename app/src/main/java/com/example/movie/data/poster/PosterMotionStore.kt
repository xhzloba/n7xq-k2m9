package com.example.movie.data.poster

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * Видеопостеры лежат на релизе `posters`, не в git и не в apk.
 * posters.json: id фильма → имя файла на том же релизе.
 */
class PosterMotionStore(
    context: Context,
    private val client: OkHttpClient,
) {
    private val dir = File(context.applicationContext.filesDir, "poster-motion").apply { mkdirs() }
    private val manifestFile = File(dir, "posters.json")
    private val names = HashMap<String, String>()
    @Volatile private var manifestLoaded = false
    private var manifestAt = 0L
    private val gate = Mutex()
    private val spots = HashMap<Int, Spot>()
    private val stateLock = Any()
    private val slotSeq = AtomicInteger()
    private val liveState = MutableStateFlow<Set<Int>>(emptySet())
    private val covers = HashMap<Int, Int>()
    private val played = HashSet<String>()
    private val asked = HashSet<Int>()
    private val clipsState = MutableStateFlow<Set<String>>(emptySet())

    /** id фильмов, у которых на релизе есть ролик. */
    val clips: StateFlow<Set<String>> = clipsState
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var inForeground = false

    /** Видимые ролики, которые в этом заходе ещё не стартовали. Новый заход — только после ухода из приложения. */
    val live: StateFlow<Set<Int>> = liveState

    init {
        scope.launch {
            while (isActive && !manifestLoaded) {
                gate.withLock { loadManifest() }
                if (!manifestLoaded) delay(15_000)
            }
        }
    }

    fun openSlot(): Int = slotSeq.incrementAndGet()

    fun report(
        slot: Int,
        movieId: String,
        place: String,
        layer: Int,
        fraction: Float,
        hero: Boolean,
        active: Boolean,
        manual: Boolean,
    ) {
        val shown = if (!manual && hero && !active) 0f else fraction
        synchronized(stateLock) {
            val dropped = manual && shown < VISIBLE && asked.remove(slot)
            val spot = spots[slot]
            if (spot == null) {
                spots[slot] = Spot(movieId, place, layer, shown, spent = false, manual = manual)
            } else {
                if (
                    !dropped &&
                    spot.movieId == movieId &&
                    spot.place == place &&
                    spot.layer == layer &&
                    spot.fraction == shown &&
                    spot.manual == manual
                ) {
                    return
                }
                spot.movieId = movieId
                spot.place = place
                spot.layer = layer
                spot.fraction = shown
                spot.manual = manual
            }
            electLocked()
        }
    }

    fun ask(slot: Int) {
        synchronized(stateLock) {
            val spot = spots[slot] ?: return
            spot.spent = false
            asked.add(slot)
            electLocked()
        }
    }

    fun openCover(layer: Int): Int {
        val token = slotSeq.incrementAndGet()
        synchronized(stateLock) {
            covers[token] = layer
            electLocked()
        }
        return token
    }

    fun dropCover(token: Int) {
        synchronized(stateLock) {
            if (covers.remove(token) == null) return
            electLocked()
        }
    }

    fun seen(slot: Int) {
        synchronized(stateLock) {
            val spot = spots[slot] ?: return
            played.add(mark(spot))
        }
    }

    fun onHostForeground(foreground: Boolean) {
        synchronized(stateLock) {
            if (foreground == inForeground) return
            inForeground = foreground
            if (!foreground) {
                publish(emptySet())
                return
            }
            played.clear()
            asked.clear()
            for (spot in spots.values) spot.spent = false
            electLocked()
        }
    }

    fun complete(slot: Int) {
        synchronized(stateLock) {
            val spot = spots[slot] ?: return
            if (spot.spent) return
            spot.spent = true
            played.add(mark(spot))
            asked.remove(slot)
            electLocked()
        }
    }

    fun drop(slot: Int) {
        synchronized(stateLock) {
            asked.remove(slot)
            if (spots.remove(slot) == null) return
            electLocked()
        }
    }

    private fun electLocked() {
        if (!inForeground || !manifestLoaded) {
            publish(emptySet())
            return
        }
        var ceiling = Int.MIN_VALUE
        for (layer in covers.values) if (layer > ceiling) ceiling = layer
        var top = ceiling
        for (spot in spots.values) {
            if (spot.fraction < VISIBLE) continue
            if (spot.layer > top) top = spot.layer
        }
        val playing = liveState.value
        val next = HashSet<Int>()
        if (top != Int.MIN_VALUE) {
            for ((slot, spot) in spots) {
                if (spot.layer != top || spot.spent || spot.fraction < VISIBLE) continue
                if (names[spot.movieId] == null) continue
                if (spot.manual) {
                    if (slot !in asked) continue
                } else if (mark(spot) in played && slot !in playing) {
                    continue
                }
                next.add(slot)
            }
        }
        publish(next)
    }

    private fun publish(next: Set<Int>) {
        if (liveState.value != next) liveState.value = next
    }

    private fun mark(spot: Spot) = spot.place + "\u0000" + spot.movieId

    private class Spot(
        var movieId: String,
        var place: String,
        var layer: Int,
        var fraction: Float,
        var spent: Boolean,
        var manual: Boolean,
    )

    suspend fun fileFor(id: String): File? = withContext(Dispatchers.IO) {
        gate.withLock {
            if (!manifestLoaded) loadManifest()
            val name = names[id] ?: return@withLock null
            val dest = File(dir, name)
            if (dest.isFile && dest.length() > 0L) return@withLock dest
            if (!download(clipUrl(name), dest)) null else dest
        }
    }

    private fun loadManifest() {
        if (manifestLoaded) return
        val now = System.currentTimeMillis()
        if (manifestAt != 0L && now - manifestAt < 60_000L) return
        val cached = manifestFile.takeIf { it.isFile }?.readText()
            ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
        if (cached != null) {
            publish(cached)
            return
        }
        manifestAt = now
        val fresh = runCatching { http(MANIFEST)?.toString(Charsets.UTF_8) }.getOrNull() ?: return
        val parsed = runCatching { json.decodeFromString<Map<String, String>>(fresh) }.getOrNull() ?: return
        manifestFile.writeText(fresh)
        publish(parsed)
    }

    private fun publish(parsed: Map<String, String>) {
        synchronized(stateLock) {
            names.clear()
            names.putAll(parsed)
            manifestLoaded = true
            val ids = parsed.keys.toSet()
            if (clipsState.value != ids) clipsState.value = ids
            electLocked()
        }
    }

    private fun download(url: String, dest: File): Boolean {
        val part = File(dir, "${dest.name}.part")
        return try {
            val bytes = http(url) ?: return false
            part.writeBytes(bytes)
            if (part.length() <= 0L) return false
            if (dest.exists()) dest.delete()
            part.renameTo(dest)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        } finally {
            if (part.exists()) part.delete()
        }
    }

    private fun http(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AmigoPlay")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val type = response.body?.contentType()?.toString().orEmpty()
            if (type.contains("text/html")) return null
            return response.body?.bytes()
        }
    }

    private companion object {
        const val RELEASE = "https://github.com/xhzloba/n7xq-k2m9/releases/download/posters"
        const val MANIFEST = "$RELEASE/posters.json"

        fun clipUrl(name: String) = "$RELEASE/$name"

        const val VISIBLE = 0.5f

        val json = Json { ignoreUnknownKeys = true }
    }
}
