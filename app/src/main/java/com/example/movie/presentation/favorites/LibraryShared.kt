package com.example.movie.presentation.favorites

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.movie.R
import com.example.movie.data.download.OfflineItem
import com.example.movie.domain.model.ContentKind
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.SeriesSub
import com.example.movie.presentation.home.components.BrandTopInset

internal val FavoritePlay = Color(0xFFE23B5A)
internal val CapsuleInk = Color.White.copy(alpha = 0.72f)
internal val LibrarySheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
internal val LibraryMenuScrim = Color(0xFF3A3A3E).copy(alpha = 0.86f)
internal val LibraryRowInk = Color(0xFFE8E8EC)

internal enum class LibrarySort(@StringRes val label: Int) {
    Year(R.string.favorites_sort_year),
    Title(R.string.favorites_sort_title),
    Rating(R.string.catalog_sort_rating),
    Release(R.string.favorites_sort_release),
}

internal enum class LibraryKind(@StringRes val label: Int) {
    Movies(R.string.favorites_movies),
    Series(R.string.favorites_series),
    Cartoons(R.string.favorites_cartoons),
    CartoonSeries(R.string.catalog_cartoon_series),
    Anime(R.string.favorites_anime),
}

internal fun SavedTitle.libraryKind(): LibraryKind? = when (type.lowercase()) {
    "movie" -> LibraryKind.Movies
    "serial", "series", "tv" -> LibraryKind.Series
    "multfilm", "mult", "cartoon" -> LibraryKind.Cartoons
    "multserial", "multseries" -> LibraryKind.CartoonSeries
    "anime" -> LibraryKind.Anime
    else -> when (kind) {
        ContentKind.Anime.name -> LibraryKind.Anime
        ContentKind.Series.name -> LibraryKind.Series
        ContentKind.Movie.name -> LibraryKind.Movies
        else -> null
    }
}

internal fun List<SavedTitle>.presentKinds(): List<LibraryKind> {
    val found = mapNotNull { it.libraryKind() }.toSet()
    return LibraryKind.entries.filter { it in found }
}

internal fun decodeFilters(raw: String): Map<String, String> {
    if (raw.isEmpty()) return emptyMap()
    return raw.split('\u001e').mapNotNull { row ->
        val split = row.indexOf('\u001f')
        if (split <= 0) null else row.substring(0, split) to row.substring(split + 1)
    }.toMap()
}

internal fun encodeFilters(filters: Map<String, String>): String =
    filters.entries.joinToString("\u001e") { "${it.key}\u001f${it.value}" }

internal fun Map<String, String>.with(key: String, value: String?): Map<String, String> {
    val next = toMutableMap()
    if (value == null) next.remove(key) else next[key] = value
    return next
}

internal const val LibraryRailCap = 15

@Composable
internal fun libraryTopPadding(): Dp {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    return top + BrandTopInset + 36.dp + 18.dp
}

@Composable
internal fun ScrollState.libraryScrim(): Float {
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    return (value / threshold).coerceIn(0f, 1f)
}

@Composable
internal fun LazyListState.libraryScrim(): Float {
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    return if (firstVisibleItemIndex > 0) 1f else (firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
}

@Composable
internal fun LazyGridState.libraryScrim(): Float {
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    return if (firstVisibleItemIndex > 0) 1f else (firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
}

internal enum class LibrarySection(@StringRes val label: Int) {
    Favorites(R.string.section_favorites),
    Continue(R.string.section_continue),
    History(R.string.section_history),
    Watched(R.string.library_watched),
    Downloads(R.string.favorites_downloaded),
    Alerts(R.string.library_alerts),
    Actors(R.string.favorites_actors),
}

internal data class OfflinePlay(val item: OfflineItem, val season: Int?, val episode: Int?)

internal fun SeriesSub.toLibrary(): SavedTitle = SavedTitle(
    id = id,
    title = title,
    posterUrl = posterUrl,
    year = year,
    type = type.ifBlank { "serial" },
)

