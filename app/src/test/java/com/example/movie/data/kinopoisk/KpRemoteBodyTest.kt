package com.example.movie.data.kinopoisk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KpRemoteBodyTest {
    @Test
    fun urlPointsAtTheRepoFile() {
        assertEquals(
            "https://raw.githubusercontent.com/xhzloba/n7xq-k2m9/main/app/src/main/assets/kinopoisk/popular-films.json",
            kpCatalogUrl("kinopoisk/popular-films.json"),
        )
    }

    @Test
    fun acceptsTheMatchingList() {
        val raw = """
            {"category":"popular-films","parsed_at":"2026-11-26T11:15:32+04:00","items":[{"kp_id":1,"title":"A"}]}
        """.trimIndent()
        assertEquals(raw, kpRemoteBody("popular-films.json", raw))
    }

    @Test
    fun rejectsAnotherCategoryOrAnEmptyList() {
        assertNull(kpRemoteBody("popular-films.json", """{"category":"top500","items":[{"kp_id":1,"title":"A"}]}"""))
        assertNull(kpRemoteBody("popular-films.json", """{"category":"popular-films","category_title":"Популярные фильмы","items":[]}"""))
        assertNull(kpRemoteBody("popular-films.json", "not json"))
    }
}
