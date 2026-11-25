package com.example.movie.data.history

import android.content.Context
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.isKinopoiskId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ViewHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mongoByKp = HashMap<String, String>()
    private val kpByMongo = HashMap<String, String>()
    private val itemsState = MutableStateFlow(read())

    val items: StateFlow<List<SavedTitle>> = itemsState.asStateFlow()

    init {
        for (item in itemsState.value) bind(item)
    }

    fun remember(item: SavedTitle) {
        if (item.id.isBlank() || item.posterUrl.isNullOrBlank()) return
        bind(item)
        val keys = item.matchKeys()
        val previous = itemsState.value.firstOrNull { it.id == item.id }
        val stored = if (item.logoUrl.isNullOrBlank()) item.copy(logoUrl = previous?.logoUrl) else item
        val next = (listOf(stored) + itemsState.value.filter { !it.matches(keys) }).take(CAP)
        write(next)
    }

    /** Вокино и Кинопоиск про один фильм. В истории остаётся одна карточка. */
    fun note(mongo: String, kp: String) {
        if (mongo.isBlank() || kp.isBlank() || mongo == kp || !kp.isKinopoiskId()) return
        if (mongoByKp[kp] == mongo) return
        mongoByKp[kp] = mongo
        kpByMongo[mongo] = kp
        write(itemsState.value)
    }

    fun replace(next: List<SavedTitle>) {
        mongoByKp.clear()
        kpByMongo.clear()
        val clean = next
            .filter { it.id.isNotBlank() && !it.posterUrl.isNullOrBlank() }
            .distinctBy { it.id }
            .take(CAP)
        for (item in clean) bind(item)
        write(clean)
    }

    fun clear() {
        mongoByKp.clear()
        kpByMongo.clear()
        write(emptyList())
    }

    fun attachLogo(id: String, logoUrl: String) {
        if (id.isBlank() || logoUrl.isBlank()) return
        val current = itemsState.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0 || current[index].logoUrl == logoUrl) return
        val next = current.toMutableList()
        next[index] = current[index].copy(logoUrl = logoUrl)
        write(next)
    }

    private fun bind(item: SavedTitle) {
        val kp = item.kpId?.takeIf { it.isNotBlank() && it.isKinopoiskId() } ?: return
        if (item.id.isKinopoiskId() || item.id == kp) return
        mongoByKp[kp] = item.id
        kpByMongo[item.id] = kp
    }

    private fun SavedTitle.matchKeys(): Set<String> {
        val kp = when {
            id.isKinopoiskId() -> id
            !kpId.isNullOrBlank() && kpId.isKinopoiskId() -> kpId
            else -> kpByMongo[id]
        }
        val mongo = if (id.isKinopoiskId()) mongoByKp[id] else id
        return setOfNotNull(id, kp, mongo)
    }

    private fun SavedTitle.matches(keys: Set<String>): Boolean {
        if (id in keys) return true
        val kp = kpId?.takeIf { it.isNotBlank() } ?: return false
        return kp in keys
    }

    private fun read(): List<SavedTitle> {
        restoreLinks()
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val decoded = runCatching { json.decodeFromString<List<SavedTitle>>(raw) }.getOrDefault(emptyList())
        val clean = decoded.filter { it.id.isNotBlank() && !it.posterUrl.isNullOrBlank() }
        if (clean.size != decoded.size) {
            prefs.edit().putString(KEY, json.encodeToString(clean)).commit()
        }
        return clean
    }

    private fun restoreLinks() {
        val raw = prefs.getString(LINKS, null) ?: return
        val decoded = runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrNull() ?: return
        for ((kp, mongo) in decoded) {
            if (!kp.isKinopoiskId() || mongo.isBlank() || mongo == kp) continue
            mongoByKp[kp] = mongo
            kpByMongo[mongo] = kp
        }
    }

    private fun write(next: List<SavedTitle>) {
        prefs.edit()
            .putString(KEY, json.encodeToString(next))
            .putString(LINKS, json.encodeToString(mongoByKp))
            .commit()
        itemsState.value = next
    }

    private companion object {
        const val FILE = "view_history"
        const val KEY = "items"
        const val LINKS = "links"
        const val CAP = 40
    }
}
