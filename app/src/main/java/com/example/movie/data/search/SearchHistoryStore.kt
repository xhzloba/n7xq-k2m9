package com.example.movie.data.search

import android.content.Context

class SearchHistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun items(): List<String> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun remember(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        val next = listOf(clean) + items().filterNot { it.equals(clean, ignoreCase = true) }
        prefs.edit().putString(KEY, next.take(12).joinToString(SEPARATOR)).apply()
    }

    fun replace(queries: List<String>) {
        val next = queries
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinctBy { it.lowercase() }
            .take(12)
        prefs.edit().putString(KEY, next.joinToString(SEPARATOR)).commit()
    }

    fun clear() {
        prefs.edit().remove(KEY).commit()
    }

    private companion object {
        const val FILE = "search_history"
        const val KEY = "queries"
        const val SEPARATOR = "\u001E"
    }
}
