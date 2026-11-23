package com.example.movie.presentation.shell

import android.app.Activity
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.syncFolderRails
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.home.HomeSection
import com.example.movie.presentation.home.HomeUiState
import com.example.movie.presentation.home.HomeViewModel
import com.example.movie.presentation.home.RailsLookTab
import com.example.movie.presentation.settings.LocalUserSettings
import kotlin.math.abs

internal class ShellController(
    sectionNameState: MutableState<String?>,
    openFolderIdState: MutableState<String?>,
    catalogListKeyState: MutableState<String?>,
    genresTypeState: MutableState<String?>,
    alertsOpenState: MutableState<Boolean>,
    initialBase: AppTab,
    initialCatalogShown: Boolean,
    initialCatalogShift: Float,
) {
    var sectionName by sectionNameState
    var openFolderId by openFolderIdState
    var catalogListKey by catalogListKeyState
    var genresType by genresTypeState
    var alertsOpen by alertsOpenState

    val searchVeil = Animatable(0f)
    var searchShown by mutableStateOf(false)
    var homeTop by mutableIntStateOf(0)
    var catalogTop by mutableIntStateOf(0)
    var favoritesTop by mutableIntStateOf(0)
    var settingsTop by mutableIntStateOf(0)
    var peek by mutableStateOf<Movie?>(null)
    var resumePlayback by mutableStateOf<ResumeItem?>(null)
    var searchCover by mutableFloatStateOf(0f)
    val sectionShift = Animatable(1f)
    var sectionShown by mutableStateOf<HomeSection?>(null)
    var barHidden by mutableStateOf(false)
    var barHeight by mutableIntStateOf(0)
    var homeNavMenu by mutableStateOf(false)
    var homeCoverAlive by mutableStateOf(false)
    var libraryNavMenu by mutableStateOf(false)
    var libraryCoverAlive by mutableStateOf(false)
    var railsLook by mutableStateOf<RailsLookTab?>(null)
    var homeDockLeft by mutableStateOf<Color?>(null)
    var homeDockRight by mutableStateOf<Color?>(null)
    var homeDockAlpha by mutableFloatStateOf(0f)
    val genresVeil = Animatable(0f)
    var genresShown by mutableStateOf(false)
    var genresHold by mutableStateOf<String?>(null)
    val listShift = Animatable(1f)
    var listShown by mutableStateOf<String?>(null)
    var stageWidth by mutableIntStateOf(0)
    var base by mutableStateOf(initialBase)
    var baseBehind by mutableStateOf<AppTab?>(null)
    val baseShift = Animatable(0f)
    var catalogShown by mutableStateOf(initialCatalogShown)
    var catalogMounted by mutableStateOf(initialCatalogShown)
    val catalogShift = Animatable(initialCatalogShift)
    var restParked by mutableStateOf(false)
    val alertsVeil = Animatable(0f)
    var alertsShown by mutableStateOf(false)
    var exitArmedAt by mutableLongStateOf(0L)

    fun section(tab: AppTab): HomeSection? =
        sectionName?.let { runCatching { HomeSection.valueOf(it) }.getOrNull() }
            ?.takeIf { tab == AppTab.Home }

    fun catalogList(tab: AppTab): String? = catalogListKey?.takeIf { tab == AppTab.Catalog }
}

@Composable
internal fun rememberShellController(
    tab: AppTab,
    handleBack: Boolean,
    alertsRequest: Int,
    onTabChange: (String) -> Unit,
    homeViewModel: HomeViewModel,
    homeState: HomeUiState,
): ShellController {
    val sectionName = rememberSaveable { mutableStateOf<String?>(null) }
    val openFolderId = rememberSaveable { mutableStateOf<String?>(null) }
    val catalogListKey = rememberSaveable { mutableStateOf<String?>(null) }
    val genresType = rememberSaveable { mutableStateOf<String?>(null) }
    val alertsOpen = rememberSaveable { mutableStateOf(false) }
    val shell = remember {
        ShellController(
            sectionNameState = sectionName,
            openFolderIdState = openFolderId,
            catalogListKeyState = catalogListKey,
            genresTypeState = genresType,
            alertsOpenState = alertsOpen,
            initialBase = if (tab == AppTab.Catalog) AppTab.Home else tab,
            initialCatalogShown = tab == AppTab.Catalog,
            initialCatalogShift = if (tab == AppTab.Catalog) 0f else 1f,
        )
    }
    val container = LocalAppContainer.current
    val railRepository = LocalUserSettings.current
    val railSettings by railRepository.settings.collectAsStateWithLifecycle()
    val libraryFolders by container.folders.items.collectAsStateWithLifecycle()
    LaunchedEffect(libraryFolders.map { it.id }, railSettings.homeRailOrder) {
        val next = railSettings.homeRailOrder.syncFolderRails(libraryFolders.map { it.id })
        if (next != railSettings.homeRailOrder) railRepository.setHomeRailOrder(next)
    }
    val catalogSearch = tab == AppTab.Catalog && homeState.searchOpen
    LaunchedEffect(catalogSearch) {
        if (catalogSearch) {
            shell.searchShown = true
            withFrameNanos { }
            shell.searchVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (shell.searchShown) {
            shell.searchVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            shell.searchShown = false
        }
    }
    val section = shell.section(tab)
    LaunchedEffect(section) {
        val move = tween<Float>(240, easing = FastOutSlowInEasing)
        if (section != null) {
            shell.sectionShown = section
            shell.sectionShift.snapTo(1f)
            withFrameNanos { }
            withFrameNanos { }
            shell.sectionShift.animateTo(0f, move)
        } else if (shell.sectionShown != null) {
            shell.sectionShift.animateTo(1f, move)
            shell.sectionShown = null
        }
    }
    val notices = LocalShelfNotices.current
    LaunchedEffect(notices.openRequest) {
        if (notices.openRequest == 0) return@LaunchedEffect
        shell.peek = null
        shell.sectionName = null
        shell.catalogListKey = null
        shell.genresType = null
        if (homeState.searchOpen) homeViewModel.closeSearch()
    }
    val genresOpen = shell.genresType != null && tab == AppTab.Catalog
    LaunchedEffect(genresOpen) {
        if (genresOpen) {
            shell.genresHold = shell.genresType
            shell.genresShown = true
            withFrameNanos { }
            shell.genresVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (shell.genresShown) {
            shell.genresVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            shell.genresShown = false
            shell.genresHold = null
        }
    }
    val catalogList = shell.catalogList(tab)
    LaunchedEffect(catalogList) {
        val move = tween<Float>(240, easing = FastOutSlowInEasing)
        if (catalogList != null) {
            shell.listShown = catalogList
            shell.listShift.snapTo(1f)
            withFrameNanos { }
            withFrameNanos { }
            shell.listShift.animateTo(0f, move)
        } else if (shell.listShown != null) {
            shell.listShift.animateTo(1f, move)
            shell.listShown = null
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!shell.catalogMounted) shell.catalogMounted = true
        shell.restParked = true
    }
    LaunchedEffect(tab) {
        val move = tween<Float>(240, easing = FastOutSlowInEasing)
        if (tab == AppTab.Catalog) {
            shell.baseShift.snapTo(0f)
            shell.baseBehind = null
            val dir = if (AppTab.Catalog.ordinal > shell.base.ordinal) 1f else -1f
            shell.catalogShown = true
            if (abs(shell.catalogShift.value) > 0.85f) shell.catalogShift.snapTo(dir)
            withFrameNanos { }
            shell.catalogShift.animateTo(0f, move)
            return@LaunchedEffect
        }
        if (shell.catalogShown) {
            if (tab != shell.base) {
                shell.base = tab
                shell.baseShift.snapTo(0f)
                shell.baseBehind = null
            }
            val dir = if (AppTab.Catalog.ordinal > tab.ordinal) 1f else -1f
            withFrameNanos { }
            withFrameNanos { }
            shell.catalogShift.animateTo(dir, move)
            shell.catalogShown = false
            return@LaunchedEffect
        }
        if (tab == shell.base) return@LaunchedEffect
        val dir = if (tab.ordinal > shell.base.ordinal) 1f else -1f
        shell.baseBehind = shell.base
        shell.base = tab
        shell.baseShift.snapTo(dir)
        withFrameNanos { }
        withFrameNanos { }
        shell.baseShift.animateTo(0f, move)
        shell.baseBehind = null
    }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(shell.peek) {
        if (shell.peek != null) keyboard?.hide()
    }
    LaunchedEffect(alertsRequest) {
        if (alertsRequest > 0) shell.alertsOpen = true
    }
    LaunchedEffect(shell.alertsOpen) {
        if (shell.alertsOpen) {
            shell.alertsShown = true
            withFrameNanos { }
            shell.alertsVeil.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
        } else if (shell.alertsShown) {
            shell.alertsVeil.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
            shell.alertsShown = false
        }
    }
    val context = LocalContext.current
    val exitHint = stringResource(R.string.exit_press_again)
    val atHomeRoot = handleBack &&
        shell.resumePlayback == null &&
        !shell.alertsShown &&
        shell.peek == null &&
        shell.sectionShown == null &&
        !shell.genresShown &&
        shell.listShown == null &&
        tab == AppTab.Home &&
        !homeState.searchOpen &&
        !shell.homeNavMenu &&
        !shell.homeCoverAlive &&
        !shell.libraryNavMenu &&
        !shell.libraryCoverAlive
    BackHandler(
        enabled = handleBack && shell.resumePlayback == null && !shell.libraryNavMenu && !shell.libraryCoverAlive &&
            (shell.alertsShown || shell.peek != null || shell.sectionShown != null || tab != AppTab.Home),
    ) {
        when {
            shell.alertsShown -> shell.alertsOpen = false
            shell.peek != null -> shell.peek = null
            tab == AppTab.Catalog && homeState.searchOpen -> homeViewModel.closeSearch()
            shell.listShown != null -> shell.catalogListKey = null
            shell.genresShown -> shell.genresType = null
            tab == AppTab.Home && shell.sectionShown != null -> shell.sectionName = null
            else -> onTabChange(AppTab.Home.name)
        }
    }
    // На корне главной: первый Back — toast, второй за 2с — выход.
    BackHandler(enabled = atHomeRoot) {
        val now = SystemClock.elapsedRealtime()
        if (now - shell.exitArmedAt < 2_000L) {
            (context as? Activity)?.finish()
        } else {
            shell.exitArmedAt = now
            Toast.makeText(context, exitHint, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(tab) {
        if (tab != AppTab.Home && tab != AppTab.Favorites) shell.peek = null
        shell.homeNavMenu = false
        shell.libraryNavMenu = false
    }
    LaunchedEffect(shell.barHidden) {
        if (shell.barHidden) {
            shell.homeNavMenu = false
            shell.libraryNavMenu = false
        }
    }
    return shell
}
