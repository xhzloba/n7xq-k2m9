package com.example.movie.domain.model

/**
 * Жанровые полки главной. Запрос без type: фильмы и сериалы.
 * [section] совпадает с именем HomeSection.
 */
data class GenreRail(
    val id: String,
    val genre: String,
    val section: String,
)

object GenreRails {
    const val Action = "action"
    const val Scifi = "scifi"
    const val Horror = "horror"
    const val Drama = "drama"
    const val Thriller = "thriller"
    const val Comedy = "comedy"
    const val Western = "western"
    const val War = "war"
    const val Fantasy = "fantasy"
    const val Melodrama = "melodrama"
    const val Crime = "crime"
    const val Detective = "detective"

    val All: List<GenreRail> = listOf(
        GenreRail(Action, "боевик", "Action"),
        GenreRail(Scifi, "фантастика", "Scifi"),
        GenreRail(Horror, "ужасы", "Horror"),
        GenreRail(Drama, "драма", "Drama"),
        GenreRail(Thriller, "триллер", "Thriller"),
        GenreRail(Comedy, "комедия", "Comedy"),
        GenreRail(Western, "вестерн", "Western"),
        GenreRail(War, "военный", "War"),
        GenreRail(Fantasy, "фэнтези", "Fantasy"),
        GenreRail(Melodrama, "мелодрама", "Melodrama"),
        GenreRail(Crime, "криминал", "Crime"),
        GenreRail(Detective, "детектив", "Detective"),
    )

    fun sectionOf(id: String): String = All.first { it.id == id }.section

    fun query(sectionName: String): CatalogQuery? =
        All.firstOrNull { it.section == sectionName }?.let { rail ->
            CatalogQuery(sort = "new", genre = rail.genre)
        }
}
