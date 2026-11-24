package com.example.movie.presentation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.zIndex
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.favorites.FavoritesScreen
import com.example.movie.presentation.home.CatalogScreen
import com.example.movie.presentation.home.GenreAll
import com.example.movie.presentation.home.HomeRoute
import com.example.movie.presentation.home.HomeViewModel
import com.example.movie.presentation.home.RailsLookTab
import com.example.movie.presentation.settings.SettingsRoute
import com.example.movie.presentation.tv.LocalTelevision
import com.example.movie.presentation.tv.remoteEntry
import com.example.movie.presentation.tv.tvWall

@Composable
internal fun ShellPager(
    tab: AppTab,
    shell: ShellController,
    handleBack: Boolean,
    onMovieClick: (String) -> Unit,
    onTabChange: (String) -> Unit,
    playResume: (String) -> Unit,
    session: Int,
    homeViewModel: HomeViewModel,
) {
    @Composable
    fun TabBody(which: AppTab, modifier: Modifier) {
        Box(modifier) {
            when (which) {
                AppTab.Home -> HomeRoute(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { shell.peek = it },
                    onOpenSection = { shell.sectionName = it.name },
                    onOpenFolder = { id ->
                        shell.sectionName = null
                        shell.openFolderId = id
                        onTabChange(AppTab.Favorites.name)
                    },
                    onPlayResume = playResume,
                    onSearchCover = { shell.searchCover = it },
                    onHeroDock = { left, right, alpha ->
                        shell.homeDockLeft = left
                        shell.homeDockRight = right
                        shell.homeDockAlpha = alpha
                    },
                    onHideBar = { shell.barHidden = it },
                    onOpenRailsLook = {
                        shell.sectionName = null
                        shell.railsLook = RailsLookTab.Look
                    },
                    onOpenAlerts = { shell.alertsOpen = true },
                    presentSearch = tab == AppTab.Home,
                    session = session,
                    scrollTop = shell.homeTop,
                )
                AppTab.Catalog -> Unit
                AppTab.Favorites -> FavoritesScreen(
                    onMovieClick = onMovieClick,
                    onMoviePeek = { shell.peek = it },
                    onPlayResume = playResume,
                    onBack = { onTabChange(AppTab.Home.name) },
                    onOpenCatalog = { onTabChange(AppTab.Catalog.name) },
                    openFolderId = shell.openFolderId,
                    onFolderOpened = { shell.openFolderId = null },
                    handleBack = handleBack,
                    scrollTop = shell.favoritesTop,
                )
                AppTab.Settings -> SettingsRoute(scrollTop = shell.settingsTop)
            }
        }
    }
    val shellCovered = shell.sectionShown != null ||
        shell.genresShown ||
        shell.listShown != null ||
        (shell.searchShown && tab == AppTab.Catalog) ||
        shell.alertsShown ||
        shell.resumePlayback != null
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { shell.stageWidth = it.width }
            .then(
                if (shell.alertsShown) {
                    Modifier.graphicsLayer { alpha = 1f - shell.alertsVeil.value }
                } else {
                    Modifier
                },
            )
            .then(
                if (shellCovered) Modifier.tvWall(true) else Modifier.remoteEntry(),
            ),
    ) {
        Box(Modifier.fillMaxSize()) {
            for (which in listOf(AppTab.Home, AppTab.Favorites, AppTab.Settings)) {
                val front = which == shell.base
                val under = which == shell.baseBehind
                if (!front && !under && (!shell.restParked || LocalTelevision.current)) continue
                key(which) {
                    val fraction: () -> Float = when {
                        front -> {
                            { shell.baseShift.value }
                        }
                        under -> {
                            { 0f }
                        }
                        else -> {
                            { 2f }
                        }
                    }
                    val shown = front || under
                    TabBody(
                        which,
                        Modifier
                            .zIndex(if (front) 1f else 0f)
                            .slideBy(fraction, shell.stageWidth)
                            .drawWithContent { if (shown) drawContent() }
                            .fillMaxSize()
                            .appBackground(),
                    )
                }
            }
        }
        if (shell.catalogMounted && (shell.catalogShown || !LocalTelevision.current)) {
            Box(
                if (shell.catalogShown) {
                    Modifier
                        .slideBy({ shell.catalogShift.value }, shell.stageWidth)
                        .fillMaxSize()
                        .appBackground()
                } else {
                    Modifier.holdOffscreen()
                },
            ) {
                CatalogScreen(
                    scrollTop = shell.catalogTop,
                    onMovieClick = onMovieClick,
                    onMoviePeek = { shell.peek = it },
                    onSearch = homeViewModel::openSearch,
                    onAlerts = { shell.alertsOpen = true },
                    onGenres = { shell.genresType = it },
                    onOpenGenre = { type, genre ->
                        shell.catalogListKey = if (genre == GenreAll) {
                            "$type:popular"
                        } else {
                            "$type:g:$genre"
                        }
                    },
                    onOpenShelf = { shell.catalogListKey = it },
                    onHideBar = { shell.barHidden = it },
                    active = tab == AppTab.Catalog,
                )
            }
        }
    }
}
