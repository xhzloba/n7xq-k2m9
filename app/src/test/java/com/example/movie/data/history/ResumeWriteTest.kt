package com.example.movie.data.history

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeWriteTest {
    @Test
    fun keepsAStoppedMovie() {
        assertEquals(ResumeWrite.Save, resumeWrite(23 * 60_000L, 120 * 60_000L, series = false))
    }

    @Test
    fun ignoresTheOpeningSeconds() {
        assertEquals(ResumeWrite.Ignore, resumeWrite(2_000L, 120 * 60_000L, series = false))
    }

    @Test
    fun dropsAFinishedMovie() {
        assertEquals(ResumeWrite.Drop, resumeWrite(118 * 60_000L, 120 * 60_000L, series = false))
    }

    @Test
    fun keepsAFinishedEpisodeOnTheSeries() {
        assertEquals(ResumeWrite.Complete, resumeWrite(39 * 60_000L, 40 * 60_000L, series = true))
    }

    @Test
    fun ignoresAShortJumpToAnotherEpisode() {
        assertEquals(ResumeWrite.Ignore, resumeWrite(2_000L, 40 * 60_000L, series = true))
    }

    @Test
    fun dropsAMovieAtTheChosenShare() {
        assertEquals(ResumeWrite.Drop, resumeWrite(80 * 60_000L, 100 * 60_000L, series = false, percent = 80))
        assertEquals(ResumeWrite.Save, resumeWrite(70 * 60_000L, 100 * 60_000L, series = false, percent = 80))
    }

    @Test
    fun savesTheOpeningSecondsWhenCountingFromTheStart() {
        assertEquals(ResumeWrite.Save, resumeWrite(2_000L, 120 * 60_000L, series = false, startMs = 0L))
        assertEquals(ResumeWrite.Ignore, resumeWrite(0L, 120 * 60_000L, series = false, startMs = 0L))
    }
}
