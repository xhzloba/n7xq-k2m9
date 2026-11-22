package com.example.movie.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PosterDateTest {
    @Test
    fun releaseDateReplacesTheYear() {
        assertEquals("23 сен. 2026", posterDateLabel(2026, "2026-09-23", full = true))
    }

    @Test
    fun defaultIsTheYear() {
        assertEquals("2026", posterDateLabel(2026, "2026-09-23", full = false))
    }

    @Test
    fun missingDateStaysAYear() {
        assertEquals("2026", posterDateLabel(2026, null, full = true))
    }
}
