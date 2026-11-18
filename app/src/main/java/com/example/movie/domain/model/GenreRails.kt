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
    const val Family = "family"
    const val Adventure = "adventure"
    const val Biography = "biography"
    const val Sport = "sport"
    const val Music = "music"
    const val Musical = "musical"
    const val Historical = "historical"

    /** Порядок полок — по названию, с первой буквы. */
    val All: List<GenreRail> = listOf(
        GenreRail(Biography, "биография", "Biography"),
        GenreRail(Action, "боевик", "Action"),
        GenreRail(Western, "вестерн", "Western"),
        GenreRail(War, "военный", "War"),
        GenreRail(Detective, "детектив", "Detective"),
        GenreRail(Drama, "драма", "Drama"),
        GenreRail(Historical, "история", "Historical"),
        GenreRail(Comedy, "комедия", "Comedy"),
        GenreRail(Crime, "криминал", "Crime"),
        GenreRail(Melodrama, "мелодрама", "Melodrama"),
        GenreRail(Music, "музыка", "Music"),
        GenreRail(Musical, "мюзикл", "Musical"),
        GenreRail(Adventure, "приключения", "Adventure"),
        GenreRail(Family, "семейный", "Family"),
        GenreRail(Sport, "спорт", "Sport"),
        GenreRail(Thriller, "триллер", "Thriller"),
        GenreRail(Horror, "ужасы", "Horror"),
        GenreRail(Scifi, "фантастика", "Scifi"),
        GenreRail(Fantasy, "фэнтези", "Fantasy"),
    )

    fun sectionOf(id: String): String = All.first { it.id == id }.section

    fun query(sectionName: String): CatalogQuery? =
        All.firstOrNull { it.section == sectionName }?.let { rail ->
            CatalogQuery(sort = "new", genre = rail.genre)
        }
}
