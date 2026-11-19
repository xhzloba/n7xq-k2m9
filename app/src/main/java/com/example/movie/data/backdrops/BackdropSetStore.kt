package com.example.movie.data.backdrops

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackdropPick(
    val off: List<String> = emptyList(),
    val own: List<String> = emptyList(),
    val goodOnly: Boolean = false,
    val width: Map<String, Int> = emptyMap(),
) {
    fun stream(api: List<String>): List<String> {
        val alive = own.filter { File(it).isFile }.map { it.toBackdropKey() }
        val chosen = api.filterNot { it in off } + alive.filterNot { it in off }
        if (!goodOnly) return chosen
        return chosen.filter { key ->
            val known = width[key]
            known == null || known >= GoodBackdropWidth
        }
    }

    fun enabled(key: String): Boolean = key !in off
}

internal const val GoodBackdropWidth = 1280

internal fun String.toBackdropKey(): String = File(this).toURI().toString()

class BackdropSetStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, "backdrops").apply { mkdirs() }
    private val index = File(root, "sets.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }
    private val state = MutableStateFlow(read())
    val sets: StateFlow<Map<String, BackdropPick>> = state.asStateFlow()

    fun toggle(movieId: String, key: String) {
        if (movieId.isBlank() || key.isBlank()) return
        update(movieId) { pick ->
            val off = if (key in pick.off) pick.off - key else pick.off + key
            pick.copy(off = off)
        }
    }

    fun setGoodOnly(movieId: String, enabled: Boolean) {
        if (movieId.isBlank()) return
        update(movieId) { it.copy(goodOnly = enabled) }
    }

    fun rememberWidth(movieId: String, key: String, width: Int) {
        if (movieId.isBlank() || key.isBlank() || width <= 0) return
        update(movieId) { pick ->
            if (pick.width[key] == width) pick else pick.copy(width = pick.width + (key to width))
        }
    }

    fun addOwn(movieId: String, uri: Uri, resolver: ContentResolver): String? {
        if (movieId.isBlank()) return null
        val dir = File(root, movieFolder(movieId)).apply { mkdirs() }
        val ext = when (resolver.getType(uri)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val dest = File(dir, "${UUID.randomUUID()}.$ext")
        val copied = runCatching {
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: return null
            dest
        }.getOrElse {
            dest.delete()
            return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(copied.path, bounds)
        val key = copied.path.toBackdropKey()
        update(movieId) { pick ->
            val width = if (bounds.outWidth > 0) pick.width + (key to bounds.outWidth) else pick.width
            pick.copy(own = pick.own + copied.path, width = width)
        }
        return copied.path
    }

    fun removeOwn(movieId: String, path: String) {
        if (movieId.isBlank() || path.isBlank()) return
        File(path).delete()
        val key = path.toBackdropKey()
        update(movieId) { pick ->
            pick.copy(
                own = pick.own - path,
                off = pick.off - key,
                width = pick.width - key,
            )
        }
    }

    suspend fun measureMissing(movieId: String, urls: List<String>) {
        val known = state.value[movieId]?.width.orEmpty()
        val pending = urls.filter { it.startsWith("http") && known[it] == null }
        if (pending.isEmpty()) return
        withContext(Dispatchers.IO) {
            pending.forEach { url ->
                val width = remoteWidth(url) ?: return@forEach
                rememberWidth(movieId, url, width)
            }
        }
    }

    private fun remoteWidth(url: String): Int? = runCatching {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", "Lumen")
        }
        try {
            conn.inputStream.use { stream ->
                val buf = ByteArray(256 * 1024)
                var filled = 0
                while (filled < buf.size) {
                    val read = stream.read(buf, filled, buf.size - filled)
                    if (read < 0) break
                    filled += read
                    imageWidth(buf, filled)?.let { return@use it }
                }
                imageWidth(buf, filled)
            }
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    private fun imageWidth(data: ByteArray, size: Int): Int? {
        if (size >= 24 && data[0] == 0x89.toByte() && data[1] == 0x50.toByte()) {
            return readInt(data, 16).takeIf { it > 0 }
        }
        if (size < 4 || data[0] != 0xFF.toByte() || data[1] != 0xD8.toByte()) return null
        var index = 2
        while (index + 8 < size) {
            if (data[index] != 0xFF.toByte()) {
                index++
                continue
            }
            val marker = data[index + 1].toInt() and 0xff
            if (marker == 0xC0 || marker == 0xC1 || marker == 0xC2) {
                return (((data[index + 7].toInt() and 0xff) shl 8) or (data[index + 8].toInt() and 0xff))
                    .takeIf { it > 0 }
            }
            if (marker == 0xD8 || marker == 0x01 || (marker in 0xD0..0xD9)) {
                index += 2
                continue
            }
            if (index + 3 >= size) return null
            val length = ((data[index + 2].toInt() and 0xff) shl 8) or (data[index + 3].toInt() and 0xff)
            if (length < 2) return null
            index += 2 + length
        }
        return null
    }

    private fun readInt(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xff) shl 24) or
            ((data[offset + 1].toInt() and 0xff) shl 16) or
            ((data[offset + 2].toInt() and 0xff) shl 8) or
            (data[offset + 3].toInt() and 0xff)

    private fun update(movieId: String, block: (BackdropPick) -> BackdropPick) {
        synchronized(index) {
            val current = state.value
            val nextPick = block(current[movieId] ?: BackdropPick())
            if (nextPick == current[movieId]) return
            val next = current + (movieId to nextPick)
            state.value = next
            runCatching { index.writeText(json.encodeToString(next)) }
        }
    }

    private fun read(): Map<String, BackdropPick> {
        if (!index.isFile) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, BackdropPick>>(index.readText())
        }.getOrDefault(emptyMap())
    }

    private fun movieFolder(id: String): String =
        id.map { char -> if (char.isLetterOrDigit() || char == '-' || char == '_') char else '_' }
            .joinToString("")
            .ifBlank { "movie" }
}
