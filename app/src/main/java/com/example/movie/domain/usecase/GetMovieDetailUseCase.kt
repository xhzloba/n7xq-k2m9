package com.example.movie.domain.usecase

import com.example.movie.domain.model.MovieDetail
import com.example.movie.domain.repository.MovieRepository

class GetMovieDetailUseCase(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(id: String, background: Boolean = false): Result<MovieDetail> =
        repository.getMovie(id, background)
}
