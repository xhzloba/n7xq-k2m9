package com.example.movie.data.backup

import com.example.movie.data.folders.LibraryFolder
import com.example.movie.domain.model.BarScrollAuto
import com.example.movie.domain.model.BarScrollOff
import com.example.movie.domain.model.BarScrollScroll
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.UserSettings
import com.example.movie.domain.model.hidesBar
import com.example.movie.domain.model.pinsBarAway
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryArchiveTest {
    @Test
    fun roundTripKeepsListsAndSettings() {
        val file = LibraryFile(
            settings = UserSettings(showLogos = false, imageParallel = 32),
            favorites = listOf(SavedTitle(id = "1", title = "Фильм", posterUrl = "https://cdn/p.jpg")),
            search = listOf("боевик"),
        )
        val back = LibraryArchive.decode(LibraryArchive.encode(file))
        assertEquals(false, back?.settings?.showLogos)
        assertEquals(32, back?.settings?.imageParallel)
        assertEquals("Фильм", back?.favorites?.single()?.title)
        assertEquals(listOf("боевик"), back?.search)
    }

    @Test
    fun foldersRoundTripAndOldFileLeavesThemOut() {
        val file = LibraryFile(
            folders = listOf(
                LibraryFolder("Ночь", "тихо", listOf(SavedTitle(id = "9", title = "Тишина"))),
            ),
        )
        val back = LibraryArchive.decode(LibraryArchive.encode(file))
        assertEquals("Ночь", back?.folders?.single()?.name)
        assertEquals("тихо", back?.folders?.single()?.description)
        assertEquals("Тишина", back?.folders?.single()?.titles?.single()?.title)
        assertEquals(true, LibraryArchive.hasFolders(LibraryArchive.encode(file)))
        assertEquals(false, LibraryArchive.hasFolders("""{"version":1}"""))
    }

    @Test
    fun junkIsNotABackup() {
        assertNull(LibraryArchive.decode("не файл"))
    }

    @Test
    fun oldSwitchOffStaysOff() {
        val back = LibraryArchive.decode("""{"version":1,"settings":{"hideBottomBarOnScroll":false}}""")
        assertEquals(BarScrollOff, back?.settings?.barScroll)
        assertEquals(false, back?.settings?.hidesBar())
    }

    @Test
    fun autoHidesOnlyTheFullList() {
        val settings = UserSettings(barScroll = BarScrollAuto)
        assertEquals(false, settings.hidesBar())
        assertEquals(false, settings.pinsBarAway(fullList = false))
        assertEquals(true, settings.pinsBarAway(fullList = true))
        assertEquals(false, UserSettings(barScroll = BarScrollScroll).pinsBarAway(fullList = true))
        val back = LibraryArchive.decode(LibraryArchive.encode(LibraryFile(settings = settings)))
        assertEquals(BarScrollAuto, back?.settings?.barScroll)
        assertEquals(BarScrollAuto, UserSettings().barScroll)
        assertEquals(true, UserSettings(barScroll = BarScrollScroll).hidesBar())
        assertEquals(false, UserSettings(barScroll = BarScrollOff).hidesBar())
    }
}
