package com.example.movie.presentation.common

import com.example.movie.domain.model.PosterQualityFull
import com.example.movie.domain.model.PosterQualityLow
import com.example.movie.domain.model.normalizedPosterQuality

/**
 * Ячейка шире этого порога — слайдер и крупный кадр. Их URL не ужимаем.
 * Ниже порога берём ближайший размер TMDB, который ещё покрывает ячейку.
 */
private const val FullPosterPx = 560

internal fun posterForCell(url: String?, targetPx: Int, quality: String): String? {
    if (url.isNullOrBlank()) return url
    if (quality.normalizedPosterQuality() == PosterQualityFull) return url
    if (targetPx <= 0 || targetPx > FullPosterPx) return url
    downsizeKpPoster(url, targetPx, quality)?.let { return it }
    val token = if (quality.normalizedPosterQuality() == PosterQualityLow) {
        "w154"
    } else {
        when {
            targetPx <= 200 -> "w185"
            targetPx <= 360 -> "w342"
            else -> "w500"
        }
    }
    return downsizePoster(url, token)
}

private val posterSize = Regex(
    """/t/p/(w92|w154|w185|w342|w500|w780|w600_and_h900_bestv2)/""",
)

private fun downsizePoster(url: String, token: String): String {
    val match = posterSize.find(url) ?: return url
    val current = match.groupValues[1]
    if (posterWidth(current) <= posterWidth(token)) return url
    return url.replaceRange(match.range, "/t/p/$token/")
}

/**
 * Кадр Кинопоиска в JSON — `600x900`, ~160 КБ. Ячейке хватает `180x270` или `300x450`.
 * `360x540` этот CDN не отдаёт. null — адрес не кинопоисковский.
 */
private val kpPosterSize = Regex("""/(\d+)x(\d+)(?=$|[?#])""")

private fun downsizeKpPoster(url: String, targetPx: Int, quality: String): String? {
    if (!url.contains("get-kinopoisk-image/")) return null
    val match = kpPosterSize.find(url) ?: return url
    val current = match.groupValues[1].toIntOrNull() ?: return url
    val token = if (quality.normalizedPosterQuality() == PosterQualityLow) {
        "180x270"
    } else {
        when {
            targetPx <= 200 -> "180x270"
            targetPx <= 360 -> "300x450"
            else -> "600x900"
        }
    }
    val next = token.substringBefore('x').toInt()
    if (current <= next) return url
    return url.replaceRange(match.range, "/$token")
}

private fun posterWidth(token: String): Int = when (token) {
    "w92" -> 92
    "w154" -> 154
    "w185" -> 185
    "w342" -> 342
    "w500" -> 500
    "w600_and_h900_bestv2" -> 600
    "w780" -> 780
    else -> Int.MAX_VALUE
}
