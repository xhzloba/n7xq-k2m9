package com.example.movie.tvui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.MovieApplication
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.MovieTheme
import kotlinx.coroutines.delay

internal class TvPosterTarget {
    var requester: FocusRequester? = null
    var poster: FocusRequester? = null
}

class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val bars = WindowCompat.getInsetsController(window, window.decorView)
        bars.hide(WindowInsetsCompat.Type.systemBars())
        bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        setContent {
            val container = (application as MovieApplication).container
            CompositionLocalProvider(
                LocalAppContainer provides container,
                LocalUserSettings provides container.userSettings,
            ) {
                MovieTheme {
                    val model: TvHomeViewModel = viewModel(factory = TvHomeViewModel.factory(container))
                    TvShell(model)
                }
            }
        }
    }
}

@Composable
private fun TvShell(model: TvHomeViewModel) {
    val container = LocalAppContainer.current
    val catalogModel: TvCatalogViewModel = viewModel(factory = TvCatalogViewModel.factory(container))
    val rails by model.rails.collectAsStateWithLifecycle()
    val catalogRails by catalogModel.rails.collectAsStateWithLifecycle()
    var place by rememberSaveable { mutableStateOf(TvPlace.Home) }
    var menuFocused by remember { mutableStateOf(false) }
    var browsing by remember { mutableStateOf(false) }
    val menuFocus = remember { List(TvSlotCount) { FocusRequester() } }
    val posterTarget = remember { TvPosterTarget() }
    var homeRail by rememberSaveable { mutableStateOf("popular") }
    val homePicks = remember { mutableStateMapOf<String, Int>() }
    var homeReset by remember { mutableIntStateOf(0) }
    val catalogRail = remember { mutableStateMapOf<String, String>() }
    val catalogPicks = remember { mutableStateMapOf<String, Int>() }
    var catalogReset by remember { mutableIntStateOf(0) }
    var returning by remember { mutableIntStateOf(0) }
    var reach by remember { mutableIntStateOf(0) }
    var shelf by remember { mutableStateOf<TvRail?>(null) }
    var shelfIndex by remember { mutableIntStateOf(0) }
    val backdrop = remember { TvBackdrop() }
    val kind = place.kind
    LaunchedEffect(kind) {
        if (kind != null) catalogModel.show(kind)
    }
    LaunchedEffect(place) {
        if (place != TvPlace.Home && place.kind == null) browsing = false
    }
    LaunchedEffect(returning, place, rails, catalogRails, shelf) {
        if (returning == 0 || shelf != null) return@LaunchedEffect
        val ready = when {
            place == TvPlace.Home -> rails.isEmpty() || rails.any { it.ready || it.movies.isNotEmpty() }
            kind != null -> catalogRails.any { it.id.startsWith("$kind:") && (it.ready || it.movies.isNotEmpty()) }
            else -> true
        }
        if (!ready) return@LaunchedEffect
        val token = returning
        repeat(12) {
            if (token != returning) return@LaunchedEffect
            val target = posterTarget.poster ?: posterTarget.requester
            val moved = runCatching { target?.requestFocus() }
            if (target != null && moved.isSuccess && moved.getOrNull() != false) return@LaunchedEffect
            delay(40)
        }
    }
    val anchor = menuFocus[place.slot]
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black),
    ) {
        TvHomeScreen(
            rails = rails,
            activeId = homeRail,
            onActiveId = { homeRail = it },
            picks = homePicks,
            reset = homeReset,
            visible = place == TvPlace.Home,
            posterTarget = posterTarget,
            menuFocus = anchor,
            menuFocused = menuFocused,
            onBrowsing = { browsing = it },
            onReachMenu = { reach++ },
            onRetry = model::reload,
            onSeeAll = {
                shelfIndex = 0
                shelf = it
            },
            hold = shelf != null,
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (place == TvPlace.Home) 1f else 0f),
        )
        TvHomeScreen(
            rails = catalogRails,
            activeId = catalogRail[kind].orEmpty(),
            onActiveId = { catalogRail[kind.orEmpty()] = it },
            picks = catalogPicks,
            reset = catalogReset,
            visible = kind != null,
            posterTarget = posterTarget,
            menuFocus = anchor,
            menuFocused = menuFocused,
            onBrowsing = { browsing = it },
            onReachMenu = { reach++ },
            onRetry = catalogModel::reload,
            onSeeAll = {
                shelfIndex = 0
                shelf = it
            },
            hold = shelf != null,
            backdrop = backdrop,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (kind != null) 1f else 0f),
        )
        when (place) {
            TvPlace.Settings -> TvSettingsScreen(
                menuFocus = anchor,
                posterTarget = posterTarget,
                onReachMenu = { reach++ },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
            )
            TvPlace.Search -> TvSearchScreen(
                menuFocus = anchor,
                posterTarget = posterTarget,
                menuFocused = menuFocused,
                onReachMenu = { reach++ },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
            )
            TvPlace.Kinopoisk -> TvPlainPage(
                title = stringResource(place.label),
                onReachMenu = { reach++ },
                posterTarget = posterTarget,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
            )
            else -> Unit
        }
        shelf?.let { rail ->
            TvAllPage(
                rail = rail,
                menuFocused = menuFocused,
                returning = returning,
                posterTarget = shelfIndex,
                onPosterTarget = { shelfIndex = it },
                onOpen = {},
                onReachMenu = { reach++ },
                onClose = {
                    shelf = null
                    returning++
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(3f),
            )
        }
        TvSideMenu(
            current = place,
            compact = browsing && !menuFocused,
            reach = reach,
            focus = menuFocus,
            onOpen = { next ->
                shelf = null
                if (next != place) {
                    if (next.kind != null && next.kind != kind) catalogReset++
                    place = next
                }
                returning++
            },
            onDismiss = { returning++ },
            onMenuFocus = { menuFocused = it },
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(4f),
        )
    }
}

@Composable
private fun TvPlainPage(
    title: String,
    onReachMenu: () -> Unit,
    posterTarget: TvPosterTarget,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    SideEffect {
        posterTarget.requester = focus
        posterTarget.poster = focus
    }
    Box(
        modifier
            .focusRequester(focus)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                    return@onPreviewKeyEvent false
                }
                val native = event.nativeKeyEvent
                if (native.repeatCount > 0 || native.isLongPress) return@onPreviewKeyEvent true
                onReachMenu()
                true
            }
            .focusProperties {
                left = FocusRequester.Cancel
                up = FocusRequester.Cancel
                down = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = ComposeColor.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
