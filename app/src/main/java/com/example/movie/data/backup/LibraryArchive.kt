package com.example.movie.data.backup

import android.content.Context
import coil.imageLoader
import com.example.movie.data.alerts.ReleaseWatchStore
import com.example.movie.data.favorites.FavoriteStore
import com.example.movie.data.favorites.WatchedStore
import com.example.movie.data.history.ResumeStore
import com.example.movie.data.history.ViewHistoryStore
import com.example.movie.data.search.SearchHistoryStore
import com.example.movie.domain.model.EpisodeProgress
import com.example.movie.domain.model.FeedShelf
import com.example.movie.domain.model.FeedWatch
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.SeriesAlert
import com.example.movie.domain.model.SeriesSub
import com.example.movie.domain.model.BarScrollOff
import com.example.movie.domain.model.BarScrollScroll
import com.example.movie.domain.model.UserSettings
import com.example.movie.domain.model.UserSettingsRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement

@Serializable
internal data class KeptMarks(val id: String, val marks: List<EpisodeProgress>)

@Serializable
internal data class LibraryFile(
    val version: Int = 1,
    val settings: UserSettings = UserSettings(),
    val favorites: List<SavedTitle> = emptyList(),
    val watched: List<SavedTitle> = emptyList(),
    val history: List<SavedTitle> = emptyList(),
    val resume: List<ResumeItem> = emptyList(),
    val marks: List<KeptMarks> = emptyList(),
    val search: List<String> = emptyList(),
    val subs: List<SeriesSub> = emptyList(),
    val alerts: List<SeriesAlert> = emptyList(),
    val feed: FeedWatch = FeedWatch(),
    val shelves: List<FeedShelf> = emptyList(),
)

internal object LibraryArchive {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(file: LibraryFile): String = json.encodeToString(file)

    fun decode(text: String): LibraryFile? {
        val parsed = runCatching { json.parseToJsonElement(text) }.getOrNull() as? JsonObject ?: return null
        val file = runCatching { json.decodeFromJsonElement<LibraryFile>(parsed) }.getOrNull() ?: return null
        val settings = parsed["settings"] as? JsonObject ?: return file
        if ("barScroll" in settings) return file
        val hide = (settings["hideBottomBarOnScroll"] as? JsonPrimitive)?.booleanOrNull ?: return file
        return file.copy(
            settings = file.settings.copy(barScroll = if (hide) BarScrollScroll else BarScrollOff),
        )
    }
}

internal class AppBackup(
    private val context: Context,
    private val settings: UserSettingsRepository,
    private val favorites: FavoriteStore,
    private val watched: WatchedStore,
    private val history: ViewHistoryStore,
    private val resume: ResumeStore,
    private val search: SearchHistoryStore,
    private val alerts: ReleaseWatchStore,
) {
    fun export(): String = LibraryArchive.encode(
        LibraryFile(
            settings = settings.settings.value,
            favorites = favorites.items.value,
            watched = watched.items.value,
            history = history.items.value,
            resume = resume.items.value,
            marks = resume.kept.value.map { (id, marks) -> KeptMarks(id, marks) },
            search = search.items(),
            subs = alerts.subs.value,
            alerts = alerts.alerts.value,
            feed = alerts.feed.value,
            shelves = alerts.shelves.value,
        ),
    )

    fun import(text: String): Boolean {
        val file = LibraryArchive.decode(text) ?: return false
        if (file.version != 1) return false
        settings.replace(file.settings)
        favorites.replace(file.favorites)
        watched.replace(file.watched)
        history.replace(file.history)
        resume.replace(file.resume, file.marks.associate { it.id to it.marks })
        search.replace(file.search)
        alerts.replace(file.subs, file.alerts, file.feed, file.shelves)
        return true
    }

    fun clearData() {
        settings.replace(UserSettings())
        favorites.replace(emptyList())
        watched.replace(emptyList())
        history.clear()
        resume.replace(emptyList(), emptyMap())
        search.clear()
        alerts.replace(emptyList(), emptyList(), FeedWatch(), emptyList())
    }

    fun clearCache() {
        val loader = context.imageLoader
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
        context.cacheDir.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
        context.externalCacheDir?.listFiles()?.forEach { runCatching { it.deleteRecursively() } }
    }
}
