package com.example.movie.presentation.common

import com.example.movie.domain.model.PosterQualityFull
import com.example.movie.domain.model.PosterQualityLight
import com.example.movie.domain.model.PosterQualityLow
import org.junit.Assert.assertEquals
import org.junit.Test

class PosterSizeTest {
    private val full = "https://proxy.vokino.pro/image/t/p/w600_and_h900_bestv2/poster.jpg"

    @Test
    fun goodQualityKeepsTheOriginalFile() {
        assertEquals(full, posterForCell(full, 300, PosterQualityFull))
    }

    @Test
    fun gridCellTakesW342() {
        assertEquals(
            "https://proxy.vokino.pro/image/t/p/w342/poster.jpg",
            posterForCell(full, 300, PosterQualityLight),
        )
    }

    @Test
    fun tinyCellTakesW185AndShelfTakesW500() {
        assertEquals(
            "https://proxy.vokino.pro/image/t/p/w185/poster.jpg",
            posterForCell(full, 100, PosterQualityLight),
        )
        assertEquals(
            "https://proxy.vokino.pro/image/t/p/w500/poster.jpg",
            posterForCell(full, 420, PosterQualityLight),
        )
    }

    @Test
    fun badQualityTakesW154ButHeroStays() {
        assertEquals(
            "https://proxy.vokino.pro/image/t/p/w154/poster.jpg",
            posterForCell(full, 300, PosterQualityLow),
        )
        assertEquals(full, posterForCell(full, 900, PosterQualityLow))
    }

    @Test
    fun heroAndBackdropStay() {
        assertEquals(full, posterForCell(full, 900, PosterQualityLight))
        assertEquals(full, posterForCell(full, 0, PosterQualityLight))
        val backdrop = "https://proxy.vokino.pro/image/t/p/w1280/hero.jpg"
        assertEquals(backdrop, posterForCell(backdrop, 300, PosterQualityLow))
        val original = "https://proxy.vokino.pro/image/t/p/original/poster.jpg"
        assertEquals(original, posterForCell(original, 300, PosterQualityLow))
    }

    @Test
    fun kinopoiskCellTakesTheSizeThatCoversIt() {
        val full = "https://cdn.example/get-kinopoisk-image/1/abc/600x900"
        assertEquals(
            "https://cdn.example/get-kinopoisk-image/1/abc/300x450",
            posterForCell(full, 300, PosterQualityLight),
        )
        assertEquals(
            "https://cdn.example/get-kinopoisk-image/1/abc/180x270",
            posterForCell(full, 100, PosterQualityLight),
        )
        assertEquals(
            "https://cdn.example/get-kinopoisk-image/1/abc/180x270",
            posterForCell(full, 300, PosterQualityLow),
        )
        assertEquals(full, posterForCell(full, 420, PosterQualityLight))
        assertEquals(full, posterForCell(full, 900, PosterQualityLight))
        assertEquals(full, posterForCell(full, 300, PosterQualityFull))
        val small = "https://cdn.example/get-kinopoisk-image/1/abc/180x270?t=1"
        assertEquals(small, posterForCell(small, 300, PosterQualityLight))
        assertEquals(
            "https://cdn.example/get-kinopoisk-image/1/abc/300x450?t=1",
            posterForCell("$full?t=1", 300, PosterQualityLight),
        )
    }

    @Test
    fun doesNotUpsizeAndKeepsQuery() {
        val small = "https://proxy.vokino.pro/image/t/p/w185/poster.jpg?t=1"
        assertEquals(small, posterForCell(small, 300, PosterQualityLight))
        assertEquals(
            "https://proxy.vokino.pro/image/t/p/w342/poster.jpg?t=1",
            posterForCell("$full?t=1", 300, PosterQualityLight),
        )
    }
}
