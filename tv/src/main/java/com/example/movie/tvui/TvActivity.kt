package com.example.movie.tvui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.movie.MovieApplication
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.AppTab
import com.example.movie.ui.theme.MovieTheme

internal class TvPosterTarget {
    var requester: FocusRequester? = null
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
    var tab by rememberSaveable { mutableStateOf(AppTab.Home) }
    var menuFocused by remember { mutableStateOf(false) }
    var browsing by remember { mutableStateOf(false) }
    val tabFocus = remember { AppTab.entries.map { FocusRequester() } }
    val posterTarget = remember { TvPosterTarget() }
    var homeRail by rememberSaveable { mutableStateOf("popular") }
    val homePicks = remember { mutableStateMapOf<String, Int>() }
    var homeReset by remember { mutableIntStateOf(0) }
    var catalogType by rememberSaveable { mutableStateOf(TvCatalogViewModel.Kinds.first().id) }
    val catalogRail = remember { mutableStateMapOf<String, String>() }
    val catalogPicks = remember { mutableStateMapOf<String, Int>() }
    var catalogReset by remember { mutableIntStateOf(0) }
    LaunchedEffect(catalogType, tab) {
        if (tab == AppTab.Catalog) catalogModel.show(catalogType)
    }
    LaunchedEffect(tab) {
        if (tab != AppTab.Home && tab != AppTab.Catalog) browsing = false
    }
    SideEffect {
        if (tab != AppTab.Home && tab != AppTab.Catalog && tab != AppTab.Settings) {
            posterTarget.requester = null
        }
    }
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
            visible = tab == AppTab.Home,
            posterTarget = posterTarget,
            menuFocus = tabFocus[AppTab.Home.ordinal],
            menuFocused = menuFocused,
            onBrowsing = { browsing = it },
            onRetry = model::reload,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (tab == AppTab.Home) 1f else 0f),
        )
        TvHomeScreen(
            rails = catalogRails,
            activeId = catalogRail[catalogType].orEmpty(),
            onActiveId = { catalogRail[catalogType] = it },
            picks = catalogPicks,
            reset = catalogReset,
            visible = tab == AppTab.Catalog,
            posterTarget = posterTarget,
            menuFocus = tabFocus[AppTab.Catalog.ordinal],
            menuFocused = menuFocused,
            onBrowsing = { browsing = it },
            onRetry = catalogModel::reload,
            categories = TvCatalogViewModel.Kinds,
            categoryId = catalogType,
            onCategory = { catalogType = it },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (tab == AppTab.Catalog) 1f else 0f),
        )
        if (tab == AppTab.Settings) {
            TvSettingsScreen(
                menuFocus = tabFocus[tab.ordinal],
                posterTarget = posterTarget,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
            )
        } else if (tab != AppTab.Home && tab != AppTab.Catalog) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(tab.label),
                    color = ComposeColor.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        TvPill(
            selected = tab,
            tabFocus = tabFocus,
            posterTarget = posterTarget,
            onSelect = { next -> if (next != tab) tab = next },
            onReselect = { next ->
                when (next) {
                    AppTab.Home -> homeReset++
                    AppTab.Catalog -> {
                        catalogType = TvCatalogViewModel.Kinds.first().id
                        catalogPicks.clear()
                        catalogRail.clear()
                        catalogReset++
                    }
                    else -> Unit
                }
            },
            onMenuFocus = { menuFocused = it },
            compact = browsing,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(3f)
                .padding(start = 8.dp),
        )
    }
}

private val IdleInk = ComposeColor(0xFFC7C7CC)

@Composable
private fun TvPill(
    selected: AppTab,
    tabFocus: List<FocusRequester>,
    posterTarget: TvPosterTarget,
    onSelect: (AppTab) -> Unit,
    onReselect: (AppTab) -> Unit,
    onMenuFocus: (Boolean) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val travel by animateFloatAsState(
        targetValue = if (compact) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "pill",
    )
    val well = lerp(26.dp, 16.dp, travel)
    val icon = lerp(15.dp, 9.dp, travel)
    val shape = RoundedCornerShape(percent = 50)
    Column(
        modifier = modifier
            .onFocusChanged { onMenuFocus(it.hasFocus) }
            .clip(shape)
            .background(ComposeColor.Black.copy(alpha = 0.38f + (0.1f - 0.38f) * travel))
            .padding(horizontal = lerp(5.dp, 3.dp, travel), vertical = lerp(7.dp, 4.dp, travel)),
        verticalArrangement = Arrangement.spacedBy(lerp(2.dp, 1.dp, travel)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val last = AppTab.entries.lastIndex
        AppTab.entries.forEachIndexed { index, tab ->
            val interaction = remember(tab) { MutableInteractionSource() }
            var focused by remember(tab) { mutableStateOf(false) }
            var reselectArmed by remember(tab) { mutableStateOf(false) }
            val lit = tab == selected || focused
            Box(
                modifier = Modifier
                    .size(well)
                    .focusRequester(tabFocus[index])
                    .onFocusChanged {
                        focused = it.isFocused
                        if (!it.isFocused) {
                            reselectArmed = false
                        } else if (tab != selected) {
                            onSelect(tab)
                            reselectArmed = false
                        } else {
                            reselectArmed = true
                        }
                    }
                    .focusProperties {
                        if (index == 0) up = FocusRequester.Cancel
                        if (index == last) down = FocusRequester.Cancel
                        right = posterTarget.requester ?: FocusRequester.Cancel
                    }
                    .clip(CircleShape)
                    .background(
                        when {
                            tab == selected -> tab.color.copy(alpha = 0.22f)
                            focused -> ComposeColor.White.copy(alpha = 0.14f)
                            else -> ComposeColor.Transparent
                        },
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {
                            if (tab == selected && reselectArmed) {
                                onReselect(tab)
                            } else {
                                if (tab != selected) onSelect(tab)
                                reselectArmed = true
                            }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val ink = if (lit) tab.color else IdleInk
                val description = stringResource(tab.label)
                if (tab.mark != 0) {
                    Icon(
                        painter = painterResource(tab.mark),
                        contentDescription = description,
                        tint = ink,
                        modifier = Modifier.size(icon),
                    )
                } else {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = description,
                        tint = ink,
                        modifier = Modifier.size(icon),
                    )
                }
            }
        }
    }
}
