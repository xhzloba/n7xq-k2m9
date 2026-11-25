package com.example.movie.data.favorites

import android.content.Context
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.isKinopoiskId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class WatchedStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val itemsState = MutableStateFlow(read())
    private val idsState = MutableStateFlow(itemsState.value.markIds())
    private val mongoByKp = HashMap<String, String>()
    private val kpByMongo = HashMap<String, String>()

    val items: StateFlow<List<SavedTitle>> = itemsState.asStateFlow()
    val ids: StateFlow<Set<String>> = idsState.asStateFlow()

    init {
        remember(itemsState.value)
    }

    fun toggle(item: SavedTitle) {
        if (item.id.isBlank()) return
        val keys = item.markKeys()
        val has = itemsState.value.any { it.marks(keys) }
        val next = if (has) {
            itemsState.value.filterNot { it.marks(keys) }
        } else {
            listOf(item.canonical()) + itemsState.value
        }
        write(next)
    }

    fun add(item: SavedTitle) {
        if (item.id.isBlank()) return
        val saved = item.canonical()
        val keys = saved.markKeys()
        val hit = itemsState.value.filter { it.marks(keys) }
        if (hit.size == 1 && hit[0].id == saved.id && hit[0].kpId == saved.kpId) return
        write(listOf(saved) + itemsState.value.filterNot { it.marks(keys) })
    }

    /** Вокино и Кинопоиск про один фильм. В просмотренном остаётся одна карточка. */
    fun note(mongo: String, kp: String) {
        if (mongo.isBlank() || kp.isBlank() || mongo == kp || !kp.isKinopoiskId()) return
        mongoByKp[kp] = mongo
        kpByMongo[mongo] = kp
        val keys = setOf(mongo, kp)
        val hit = itemsState.value.filter { it.marks(keys) }
        if (hit.isEmpty()) return
        if (hit.size == 1 && hit[0].id == mongo && hit[0].kpId == kp) return
        val merged = hit.first().copy(id = mongo, kpId = kp)
        write(listOf(merged) + itemsState.value.filterNot { it.marks(keys) })
    }

    fun replace(next: List<SavedTitle>) {
        write(next.filter { it.id.isNotBlank() }.distinctBy { it.id })
    }

    fun remove(id: String) {
        if (id.isBlank() || !idsState.value.contains(id)) return
        write(itemsState.value.filterNot { it.id == id || it.kpId == id })
    }

    private fun write(next: List<SavedTitle>) {
        prefs.edit().putString(KEY, json.encodeToString(next)).commit()
        itemsState.value = next
        idsState.value = next.markIds()
        remember(next)
    }

    private fun remember(items: List<SavedTitle>) {
        for (item in items) {
            val kp = item.kpId?.takeIf { it.isNotBlank() } ?: continue
            mongoByKp[kp] = item.id
            kpByMongo[item.id] = kp
        }
    }

    private fun SavedTitle.markKeys(): Set<String> {
        val kp = kpId?.takeIf { it.isNotBlank() }
            ?: id.takeIf { it.isKinopoiskId() }?.takeIf { mongoByKp.containsKey(it) }
        val mongo = when {
            kp != null && id != kp -> id
            kp != null -> mongoByKp[kp]
            else -> kpByMongo[id]?.let { id }
        }
        return setOfNotNull(id, kpId, kp, mongo, kp?.let { mongoByKp[it] }, mongo?.let { kpByMongo[it] })
    }

    private fun SavedTitle.canonical(): SavedTitle {
        val keys = markKeys()
        val kp = keys.firstOrNull { it.isKinopoiskId() && mongoByKp.containsKey(it) }
            ?: kpId?.takeIf { it.isNotBlank() }
        val mongo = kp?.let { mongoByKp[it] } ?: return this
        return copy(id = mongo, kpId = kp)
    }

    private fun SavedTitle.marks(keys: Set<String>): Boolean =
        id in keys || (kpId != null && kpId in keys)

    private fun read(): List<SavedTitle> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SavedTitle>>(raw) }.getOrDefault(emptyList())
    }

    private companion object {
        const val FILE = "watched"
        const val KEY = "items"

        fun List<SavedTitle>.markIds(): Set<String> = flatMap {
            listOfNotNull(it.id, it.kpId?.takeIf { kp -> kp.isNotBlank() })
        }.toSet()
    }
}
