package com.example.movie.data.palette

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WashTone(
    val intensity: Float = 1f,
    val light: Float = 0f,
    /** Свет каждого угла. 0 — яркость как у цвета. Порядок как у punch. */
    val lights: List<Float> = listOf(0f, 0f, 0f, 0f),
    val corner: Float = 0f,
    val punch: List<Float> = listOf(0f, 0f, 0f, 0f),
    val pick: List<Int> = listOf(0, 0, 0, 0),
) {
    fun plain(): Boolean =
        intensity == 1f &&
            corner == 0f &&
            lights.all { it == 0f } &&
            punch.all { it == 0f } &&
            pick.all { it == 0 }
}

internal fun washToneKey(movieId: String, frame: String?): String {
    if (frame.isNullOrBlank()) return movieId
    return "$movieId\n$frame"
}

class WashToneStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "wash-tones.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val state = MutableStateFlow(read())
    val tones: StateFlow<Map<String, WashTone>> = state.asStateFlow()

    fun setIntensity(id: String, value: Float) = edit(id) { it.copy(intensity = value.coerceIn(0.45f, 1.55f)) }

    fun setLight(id: String, index: Int, value: Float) = edit(id) { tone ->
        val next = tone.lights.padLight().toMutableList()
        next[index.coerceIn(0, 3)] = value.coerceIn(-1f, 1f)
        tone.copy(lights = next, light = 0f)
    }

    fun setPunch(id: String, index: Int, value: Float) = edit(id) { tone ->
        val next = tone.punch.pad().toMutableList()
        next[index.coerceIn(0, 3)] = value.coerceIn(-1f, 1f)
        tone.copy(punch = next)
    }

    fun setPick(id: String, index: Int, color: Int) = edit(id) { tone ->
        val next = tone.pick.padPick().toMutableList()
        next[index.coerceIn(0, 3)] = color
        tone.copy(pick = next)
    }

    fun copyCorner(id: String, from: Int, to: Int, algorithmBase: Int) = edit(id) { tone ->
        val src = from.coerceIn(0, 3)
        val dst = to.coerceIn(0, 3)
        if (src == dst) return@edit tone
        val punch = tone.punch.pad().toMutableList()
        val pick = tone.pick.padPick().toMutableList()
        val lights = tone.lights.padLight().toMutableList()
        punch[dst] = punch[src]
        lights[dst] = lights[src]
        val base = pick[src].takeIf { it != 0 } ?: algorithmBase
        if (base != 0) pick[dst] = base
        tone.copy(punch = punch, pick = pick, lights = lights, light = 0f)
    }

    fun reset(id: String) {
        if (id.isBlank()) return
        synchronized(file) {
            val next = state.value - id
            state.value = next
            write(next)
        }
    }

    fun clearAll() {
        synchronized(file) {
            state.value = emptyMap()
            if (file.exists()) file.delete()
        }
    }

    fun replace(tones: Map<String, WashTone>) {
        synchronized(file) {
            val next = tones.mapValues { (_, tone) -> tone.normalized() }.filterValues { !it.plain() }
            state.value = next
            write(next)
        }
    }

    private fun edit(id: String, transform: (WashTone) -> WashTone) {
        if (id.isBlank()) return
        synchronized(file) {
            val tone = transform(state.value[id] ?: WashTone()).normalized()
            val next = if (tone.plain()) state.value - id else state.value + (id to tone)
            state.value = next
            write(next)
        }
    }

    private fun read(): Map<String, WashTone> {
        if (!file.isFile) return emptyMap()
        return runCatching {
            json.decodeFromString<Map<String, WashTone>>(file.readText()).mapValues { (_, tone) ->
                tone.normalized()
            }
        }.getOrDefault(emptyMap())
    }

    private fun write(tones: Map<String, WashTone>) {
        if (tones.isEmpty()) {
            if (file.exists()) file.delete()
            return
        }
        runCatching { file.writeText(json.encodeToString(tones)) }
    }
}

private fun WashTone.normalized(): WashTone = copy(
    intensity = intensity.coerceIn(0.45f, 1.55f),
    light = 0f,
    corner = corner.coerceIn(0f, 1f),
    punch = punch.pad(),
    lights = lights.padLight(),
    pick = pick.padPick(),
)

private fun List<Float>.pad(): List<Float> = List(4) { getOrElse(it) { 0f }.coerceIn(-1f, 1f) }

private fun List<Float>.padLight(): List<Float> = pad()

private fun List<Int>.padPick(): List<Int> = List(4) { getOrElse(it) { 0 } }
