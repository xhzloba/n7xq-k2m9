package com.example.movie.data.folders

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LibraryFolder(
    val name: String,
    val description: String = "",
)

class FolderStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val itemsState = MutableStateFlow(read())

    val items: StateFlow<List<LibraryFolder>> = itemsState.asStateFlow()

    fun replace(next: List<LibraryFolder>) {
        val clean = next
            .map { it.copy(name = it.name.trim(), description = it.description.trim()) }
            .filter { it.name.isNotEmpty() }
        prefs.edit().putString(KEY, json.encodeToString(clean)).commit()
        itemsState.value = clean
    }

    private fun read(): List<LibraryFolder> {
        val raw = prefs.getString(KEY, null)
        if (raw != null) {
            return runCatching { json.decodeFromString<List<LibraryFolder>>(raw) }
                .getOrDefault(emptyList())
                .filter { it.name.isNotBlank() }
        }
        return migrate()
    }

    private fun migrate(): List<LibraryFolder> {
        val legacy = app.getSharedPreferences(LEGACY, Context.MODE_PRIVATE)
        val text = legacy.getString(LEGACY_KEY, null).orEmpty()
        val parsed = text.split('\u001e').mapNotNull { row ->
            if (row.isBlank()) return@mapNotNull null
            val split = row.indexOf('\u001f')
            if (split < 0) {
                LibraryFolder(row, "")
            } else {
                LibraryFolder(row.substring(0, split), row.substring(split + 1))
            }
        }.filter { it.name.isNotBlank() }
        prefs.edit().putString(KEY, json.encodeToString(parsed)).commit()
        legacy.edit().remove(LEGACY_KEY).commit()
        return parsed
    }

    private companion object {
        const val FILE = "folders"
        const val KEY = "items"
        const val LEGACY = "library_folders"
        const val LEGACY_KEY = "names"
    }
}
