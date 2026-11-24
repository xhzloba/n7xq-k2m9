package com.example.movie.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogShelfKeyTest {
    @Test
    fun russianListIsNotThePlainPopularList() {
        val opened = openedCatalogShelf(CatalogSort.Russian.listKey("movie"))
        assertEquals("movie", opened?.type)
        assertEquals("popular", opened?.sort)
        assertEquals("Россия", opened?.country)
        assertEquals(CatalogSort.Russian.title, opened?.title)
    }

    @Test
    fun popularListHasNoCountry() {
        val opened = openedCatalogShelf(CatalogSort.Popular.listKey("movie"))
        assertEquals("popular", opened?.sort)
        assertNull(opened?.country)
        assertEquals(CatalogSort.Popular.title, opened?.title)
    }

    @Test
    fun genreAllStillOpensPopular() {
        val opened = openedCatalogShelf("movie:popular")
        assertEquals(CatalogSort.Popular.title, opened?.title)
        assertNull(opened?.country)
    }

    @Test
    fun genreKeyIsNotAShelf() {
        assertNull(openedCatalogShelf("movie:g:драма"))
    }

    @Test
    fun russianSerialListKeepsCountry() {
        val opened = openedCatalogShelf(CatalogSort.Russian.listKey("serial"))
        assertEquals("serial", opened?.type)
        assertEquals("popular", opened?.sort)
        assertEquals("Россия", opened?.country)
    }

    @Test
    fun russianSitsAfterUpdatesOnMoviesAndSeries() {
        for (type in listOf("movie", "serial")) {
            val shelves = CatalogSort.forType(type)
            val index = shelves.indexOf(CatalogSort.Russian)
            assertEquals(CatalogSort.Updates, shelves[index - 1])
            assertEquals(CatalogSort.Best, shelves[index + 1])
        }
        assertEquals(false, CatalogSort.Russian in CatalogSort.forType("anime"))
        assertEquals(false, CatalogSort.Russian in CatalogSort.forType("multfilm"))
        assertEquals(false, CatalogSort.Russian in CatalogSort.forType("multserial"))
    }
}
