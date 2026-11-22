package com.example.movie.domain.model

enum class ContentKind {
    Movie,
    Series,
    Anime,
    Other,
}

data class Movie(
    val id: String,
    val title: String,
    val originalTitle: String,
    val year: Int?,
    val overview: String,
    val genres: String,
    val country: String,
    val director: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val ratingKp: Float?,
    val ratingImdb: Float?,
    val age: Int?,
    val kind: ContentKind,
    val camrip: Boolean,
    val type: String = "",
    /** ISO `YYYY-MM-DD` из каталога. Пусто — на обложке остаётся год. */
    val releaseDate: String? = null,
)
