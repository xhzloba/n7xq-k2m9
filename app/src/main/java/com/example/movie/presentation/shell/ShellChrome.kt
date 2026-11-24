package com.example.movie.presentation.shell

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.FranchiseSeason
import com.example.movie.domain.model.episodeFinished
import com.example.movie.domain.model.episodeMarks
import com.example.movie.domain.model.fullyWatched
import com.example.movie.domain.model.isFolderRail
import com.example.movie.domain.model.pinsBarAway
import com.example.movie.domain.model.toSaved
import com.example.movie.domain.model.UserSettings
import com.example.movie.presentation.alerts.AlertsScreen
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.common.appChrome
import com.example.movie.presentation.common.dockInk
import com.example.movie.presentation.detail.WatchPlayer
import com.example.movie.presentation.detail.resumeWatchUrl
import com.example.movie.presentation.favorites.LibraryOrderCover
import com.example.movie.presentation.home.HomeRails
import com.example.movie.presentation.home.HomeRailsCover
import com.example.movie.presentation.home.HomeRailsLookSheet
import com.example.movie.presentation.home.HomeUiState
import com.example.movie.presentation.home.HomeViewModel
import com.example.movie.presentation.home.MoviePeek
import com.example.movie.presentation.home.SearchPage
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.tv.remoteEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun BoxScope.ShellChrome(
    tab: AppTab,
    shell: ShellController,
    homeState: HomeUiState,
    homeViewModel: HomeViewModel,
    onMovieClick: (String) -> Unit,
    onTabChange: (String) -> Unit,
    playResume: (String) -> Unit,
) {
    val searchDim = if (tab == AppTab.Catalog) shell.searchVeil.value else shell.searchCover
    val genresAlpha = if (shell.genresShown) shell.genresVeil.value else 0f
    val barAlpha = (1f - searchDim) * (1f - genresAlpha) * (1f - shell.alertsVeil.value)
    val barSettings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val pinBar = barSettings.pinsBarAway(shell.sectionShown != null || shell.listShown != null)
    LaunchedEffect(tab, shell.sectionShown, shell.listShown, pinBar) { shell.barHidden = pinBar }
    val hide by animateFloatAsState(
        targetValue = if (shell.barHidden) 1f else 0f,
        animationSpec = if (pinBar) snap() else tween(220),
        label = "barHide",
    )
    var homeAnchor by remember { mutableStateOf<Rect?>(null) }
    val hintSettings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val hintRepository = LocalUserSettings.current
    var hintLive by remember { mutableStateOf(false) }
    val homeQuiet = tab == AppTab.Home &&
        shell.sectionShown == null &&
        !shell.homeNavMenu &&
        !shell.homeCoverAlive &&
        !shell.libraryNavMenu &&
        !shell.libraryCoverAlive &&
        shell.resumePlayback == null &&
        shell.peek == null &&
        !shell.searchShown &&
        !shell.alertsShown &&
        barAlpha > 0.5f &&
        hide < 0.5f
    LaunchedEffect(hintSettings.hints, hintSettings.homeHintShown, homeQuiet) {
        if (hintSettings.hints && !hintSettings.homeHintShown && homeQuiet && !hintLive) {
            hintLive = true
            hintRepository.setHomeHintShown(true)
        } else if (!homeQuiet && hintLive) {
            hintLive = false
        }
    }
    val showHomeHint = hintLive && homeQuiet
    var catalogHintLive by remember { mutableStateOf(false) }
    val catalogQuiet = tab == AppTab.Catalog &&
        shell.catalogListKey == null &&
        shell.genresType == null &&
        shell.peek == null &&
        !shell.searchShown &&
        !shell.alertsShown &&
        shell.resumePlayback == null &&
        barAlpha > 0.5f &&
        hide < 0.5f
    LaunchedEffect(
        hintSettings.hints,
        hintSettings.catalogHintSeen,
        catalogQuiet,
        shell.watchingAnchor,
        shell.catalogHintGone,
    ) {
        if (
            hintSettings.hints &&
            !hintSettings.catalogHintSeen &&
            catalogQuiet &&
            shell.watchingAnchor != null &&
            !catalogHintLive
        ) {
            shell.catalogHintGone = false
            catalogHintLive = true
            hintRepository.setCatalogHintSeen(true)
        } else if (!catalogQuiet || shell.catalogHintGone) {
            catalogHintLive = false
        }
    }
    val showCatalogHint = catalogHintLive && catalogQuiet && shell.watchingAnchor != null
    LaunchedEffect(hintSettings.hints, hintSettings.homeHintShown, hintSettings.catalogHintSeen) {
        if (hintSettings.hints && hintSettings.homeHintShown && hintSettings.catalogHintSeen) {
            hintRepository.setHints(false)
        }
    }
    if (barAlpha > 0.02f && shell.resumePlayback == null) {
        val fromHome = shell.sectionShown != null
        val settings = hintSettings
        val hintScope = rememberCoroutineScope()
        val onHome = tab == AppTab.Home && shell.sectionShown == null
        val shelvesOff = settings.shelvesHidden()
        val washBar = settings.heroPosterColors &&
            settings.heroPosterPage &&
            onHome &&
            !shelvesOff
        AppBottomBar(
            selected = if (fromHome) null else tab,
            onSelect = { next ->
                shell.homeNavMenu = false
                shell.libraryNavMenu = false
                if (fromHome && next == AppTab.Home) shell.sectionName = null
                else onTabChange(next.name)
            },
            onReselect = { next ->
                when (next) {
                    AppTab.Home -> shell.homeTop++
                    AppTab.Catalog -> shell.catalogTop++
                    AppTab.Favorites -> shell.favoritesTop++
                    AppTab.Settings -> shell.settingsTop++
                }
            },
            onHomeHold = {
                shell.libraryNavMenu = false
                if (!shell.homeNavMenu && showHomeHint) {
                    hintRepository.setHomeHintSeen(true)
                    hintScope.launch {
                        delay(180)
                        shell.homeNavMenu = true
                    }
                } else {
                    shell.homeNavMenu = !shell.homeNavMenu
                }
            },
            onLibraryHold = {
                shell.homeNavMenu = false
                shell.libraryNavMenu = !shell.libraryNavMenu
            },
            onHomeAnchor = { homeAnchor = it },
            washLeft = shell.homeDockLeft.takeIf { washBar },
            washRight = shell.homeDockRight.takeIf { washBar },
            washAlpha = if (washBar) shell.homeDockAlpha else 0f,
            clear = onHome && shelvesOff,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { shell.barHeight = it.height }
                .offset { IntOffset(0, (shell.barHeight * hide).toInt()) }
                .graphicsLayer { alpha = barAlpha },
        )
    }
    homeAnchor?.let { anchor ->
        HomeHoldHint(
            anchor = anchor,
            active = showHomeHint,
        )
    }
    shell.watchingAnchor?.let { anchor ->
        CatalogShelfHint(
            anchor = anchor,
            active = showCatalogHint,
        )
    }
    val flyoutSettings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val flyoutOnHome = tab == AppTab.Home && shell.sectionShown == null
    val flyoutShelvesOff = flyoutSettings.shelvesHidden()
    val flyoutWash = flyoutSettings.heroPosterColors &&
        flyoutSettings.heroPosterPage &&
        flyoutOnHome &&
        !flyoutShelvesOff
    HomeRailsCover(
        visible = shell.homeNavMenu && barAlpha > 0.02f,
        color = if (flyoutWash) {
            (shell.homeDockLeft ?: WashDockFallback).dockInk()
        } else {
            appChrome()
        },
        onAlive = { shell.homeCoverAlive = it },
        onDismiss = { shell.homeNavMenu = false },
    )
    LibraryOrderCover(
        visible = shell.libraryNavMenu && barAlpha > 0.02f,
        color = if (flyoutWash) {
            (shell.homeDockLeft ?: WashDockFallback).dockInk()
        } else {
            appChrome()
        },
        onAlive = { shell.libraryCoverAlive = it },
        onDismiss = { shell.libraryNavMenu = false },
    )
    shell.railsLook?.let { look ->
        HomeRailsLookSheet(
            initial = look,
            onDismiss = { shell.railsLook = null },
        )
    }
    if (shell.alertsShown) {
        AlertsScreen(
            onMovieClick = onMovieClick,
            onMoviePeek = { shell.peek = it },
            onBack = { shell.alertsOpen = false },
            modifier = Modifier
                .remoteEntry()
                .fillMaxSize()
                .graphicsLayer { alpha = shell.alertsVeil.value }
                .appBackground(),
        )
    }
    if (shell.searchShown && tab == AppTab.Catalog) {
        SearchPage(
            query = homeState.searchQuery,
            results = homeState.searchResults,
            searching = homeState.searching,
            loadingMore = homeState.searchLoadingMore,
            end = homeState.searchEnd,
            error = homeState.searchError,
            recent = homeState.recentQueries,
            popular = homeState.searchPopular,
            popularReady = homeState.searchPopularReady,
            onQuery = homeViewModel::search,
            onClose = homeViewModel::closeSearch,
            onClearRecent = homeViewModel::clearRecent,
            onLoadMore = homeViewModel::loadMoreSearch,
            onRetry = homeViewModel::retrySearch,
            onMovieClick = onMovieClick,
            onMoviePeek = { shell.peek = it },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shell.searchVeil.value },
        )
    }
    MoviePeek(
        movie = shell.peek,
        onDismiss = { shell.peek = null },
        onOpen = { id ->
            shell.peek = null
            onMovieClick(id)
        },
        onContinue = { id ->
            shell.peek = null
            playResume(id)
        },
    )
    val playing = shell.resumePlayback
    if (playing != null) {
        val container = LocalAppContainer.current
        val scope = rememberCoroutineScope()
        val playerSettings = LocalUserSettings.current.settings.collectAsStateWithLifecycle().value
        val watchedPercent = playerSettings.watchedPercent
        val progressStartMs = playerSettings.progressStartMs
        var aired by remember(playing.id) { mutableStateOf<List<FranchiseSeason>?>(null) }
        WatchPlayer(
            url = resumeWatchUrl(playing.franchiseId, playing.season, playing.episode),
            title = playing.title,
            logoUrl = playing.logoUrl,
            startPositionMs = playing.positionMs,
            onProgress = { position, duration, season, episode ->
                if (season == null && episode == null && (playing.season == null && playing.episode == null)) {
                    container.watched.remove(playing.id)
                }
                val next = playing.copy(
                    positionMs = position,
                    durationMs = duration,
                    season = season ?: playing.season,
                    episode = episode ?: playing.episode,
                )
                val series = next.season != null && next.episode != null
                if (series && aired == null && episodeFinished(position, duration, watchedPercent)) {
                    aired = emptyList()
                    scope.launch {
                        val seasons = container.getMovieDetail(next.id).getOrNull()?.franchise?.seasons.orEmpty()
                        aired = seasons
                        val stored = container.resume.items.value.firstOrNull { it.id == next.id }
                        val marks = stored?.episodeMarks()
                            ?: container.resume.kept.value[next.id].orEmpty()
                        if (seasons.fullyWatched(marks)) {
                            container.watched.add((stored ?: next).toSaved())
                            container.favorites.remove(next.id)
                            container.resume.forget(next.id)
                        }
                    }
                }
                val finished = container.resume.record(next, watchedPercent, progressStartMs) { stored ->
                    aired.orEmpty().fullyWatched(stored.episodeMarks())
                }
                if (finished) {
                    container.watched.add(next.toSaved())
                    container.favorites.remove(next.id)
                }
            },
            onDismiss = { shell.resumePlayback = null },
        )
    }
}

private fun UserSettings.shelvesHidden(): Boolean =
    HomeRails.All.none { homeRailVisible(it.id) } &&
        homeRailOrder.none { it.isFolderRail() && homeRailVisible(it) }
