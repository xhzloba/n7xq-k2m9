package com.example.movie.presentation.detail

import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.MovieDetail

internal object DetailSeed {
    private val movies = object : LinkedHashMap<String, Movie>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Movie>?) = size > 48
    }

    fun put(movie: Movie) {
        if (movie.id.isBlank() || movie.title.isBlank()) return
        synchronized(movies) { movies[movie.id] = movie }
    }

    fun get(id: String): Movie? = synchronized(movies) { movies[id] }
}

internal fun Movie.toPreviewDetail(): MovieDetail {
    val still = backdropUrl?.takeIf { it.isNotBlank() } ?: posterUrl?.takeIf { it.isNotBlank() }
    return MovieDetail(
        id = id,
        title = title,
        logoUrl = logoUrl,
        posterUrl = posterUrl,
        year = year,
        runtimeMinutes = null,
        age = age,
        genres = genres.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        overview = overview,
        ratingKp = ratingKp,
        ratingImdb = ratingImdb,
        backdrops = listOfNotNull(still),
        type = type,
        director = director,
        country = country,
    )
}
