package com.example.movie.data.kinopoisk

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun sliceReadsOnePageAndLeavesTheRest() {
        val raw = pageJson(40)
        val first = kpSlice(raw, 0, KpPageSize)
        assertEquals(KpPageSize, first.movies.size)
        assertEquals("1", first.movies.first().id)
        assertEquals("15", first.movies.last().id)
        assertEquals(1, first.ranks.getValue("1").position)
        assertFalse(first.end)
        assertEquals(KpPageSize, first.nextOffset)
        val second = kpSlice(raw, first.nextOffset, KpPageSize)
        assertEquals("16", second.movies.first().id)
        assertEquals("30", second.movies.last().id)
        val tail = kpSlice(raw, 30, KpPageSize)
        assertEquals(10, tail.movies.size)
        assertEquals("40", tail.movies.last().id)
        assertTrue(tail.end)
        assertEquals(first.movies + second.movies, (first + second).movies)
    }

    @Test
    fun assetFileOpensAsAPageNotTheWholeList() {
        val file = File("src/main/assets/kinopoisk/popular-films.json")
        val raw = file.readText()
        val first = kpSlice(raw, 0, KpPageSize)
        assertEquals("popular-films", first.id)
        assertEquals(KpPageSize, first.movies.size)
        assertEquals("5927239", first.movies.first().id)
        assertFalse(first.end)
        var slice = first
        var guard = 0
        while (!slice.end && guard < 80) {
            slice += kpSlice(raw, slice.nextOffset, KpPageSize)
            guard++
        }
        assertTrue(slice.end)
        assertEquals(1000, slice.movies.size)
        assertEquals("5927239", slice.movies.first().id)
    }

    private fun pageJson(count: Int): String {
        val items = (1..count).joinToString(",") { index ->
            """{"kp_id":$index,"title":"T$index","position":$index}"""
        }
        return """{"category":"popular-films","category_title":"Популярные фильмы","parsed_at":"2026-11-26T11:15:32+04:00","items":[$items]}"""
    }
}
