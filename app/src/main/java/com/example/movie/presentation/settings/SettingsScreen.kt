package com.example.movie.presentation.settings

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.HapticFeedbackConstants
import com.example.movie.BuildConfig
import com.example.movie.R
import com.example.movie.domain.model.UserSettings
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.update.LocalAppUpdate
import com.example.movie.presentation.update.UpdateAlert
import com.example.movie.presentation.update.UpdateBadge
import com.example.movie.presentation.update.UpdateNotice
import com.example.movie.presentation.update.UpdatePhase
import com.example.movie.presentation.update.downloadStatus
import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.model.DownloadProgress
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.presentation.common.THEME_BLACK
import com.example.movie.presentation.common.THEME_CINEMA
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface
import kotlinx.coroutines.flow.first

private enum class SettingsPage {
    Root,
    Theme,
    Posters,
    Navigation,
    Color,
    Detail,
    Player,
    Sensors,
    About,
    ;

    val label: Int
        get() = when (this) {
            Root -> R.string.settings_title
            Theme -> R.string.settings_group_theme
            Posters -> R.string.settings_group_posters
            Navigation -> R.string.settings_group_navigation
            Color -> R.string.settings_group_color
            Detail -> R.string.settings_group_detail
            Player -> R.string.settings_group_player
            Sensors -> R.string.settings_sensors
            About -> R.string.settings_about
        }

    val hint: Int
        get() = when (this) {
            Root -> R.string.settings_title
            Theme -> R.string.settings_menu_theme
            Posters -> R.string.settings_menu_posters
            Navigation -> R.string.settings_menu_navigation
            Color -> R.string.settings_menu_color
            Detail -> R.string.settings_menu_detail
            Player -> R.string.settings_menu_player
            Sensors -> R.string.settings_menu_sensors
            About -> R.string.settings_version_value
        }

    val icon: ImageVector
        get() = when (this) {
            Root -> ShellIcons.settings
            Theme -> ShellIcons.brush
            Posters -> ShellIcons.shelf
            Navigation -> ShellIcons.navigate
            Color -> ShellIcons.palette
            Detail -> ShellIcons.movie
            Player -> ShellIcons.play
            Sensors -> ShellIcons.vibrate
            About -> ShellIcons.info
        }
}

@Composable
fun SettingsRoute() {
    val repository = LocalUserSettings.current
    val settings by repository.settings.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onExtractBackdrop = repository::setExtractBackdrop,
        onExtractPlex = repository::setExtractPlex,
        onPlexBackdrop = repository::setPlexBackdrop,
        onHeroPosterColors = repository::setHeroPosterColors,
        onHeroPosterPage = repository::setHeroPosterPage,
        onShowLogos = repository::setShowLogos,
        onWatchedBadge = repository::setWatchedBadge,
        onShowPosterTitles = repository::setShowPosterTitles,
        onWhiteTitles = repository::setWhiteTitles,
        onHideBottomBarOnScroll = repository::setHideBottomBarOnScroll,
        onNavLabels = repository::setNavLabels,
        onSliderShade = repository::setSliderShade,
        onLargePosters = repository::setLargePosters,
        onAutoBackdrop = repository::setAutoBackdrop,
        onWatchGlow = repository::setWatchGlow,
        onDetailIris = repository::setDetailIris,
        onHaptic = repository::setHaptic,
        onAppTheme = repository::setAppTheme,
        onWatchedPercent = repository::setWatchedPercent,
        onCountFromStart = repository::setCountFromStart,
    )
}

@Composable
private fun SettingsScreen(
    settings: UserSettings,
    onExtractBackdrop: (Boolean) -> Unit,
    onExtractPlex: (Boolean) -> Unit,
    onPlexBackdrop: (Boolean) -> Unit,
    onHeroPosterColors: (Boolean) -> Unit,
    onHeroPosterPage: (Boolean) -> Unit,
    onShowLogos: (Boolean) -> Unit,
    onWatchedBadge: (Boolean) -> Unit,
    onShowPosterTitles: (Boolean) -> Unit,
    onWhiteTitles: (Boolean) -> Unit,
    onHideBottomBarOnScroll: (Boolean) -> Unit,
    onNavLabels: (Boolean) -> Unit,
    onSliderShade: (Boolean) -> Unit,
    onLargePosters: (Boolean) -> Unit,
    onAutoBackdrop: (Boolean) -> Unit,
    onWatchGlow: (Boolean) -> Unit,
    onDetailIris: (Boolean) -> Unit,
    onHaptic: (Boolean) -> Unit,
    onAppTheme: (String) -> Unit,
    onWatchedPercent: (Int) -> Unit,
    onCountFromStart: (Boolean) -> Unit,
) {
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.Root.name) }
    val page = SettingsPage.entries.firstOrNull { it.name == pageName } ?: SettingsPage.Root
    var shown by remember { mutableStateOf(SettingsPage.Root) }
    val shift = remember { Animatable(1f) }
    var stageWidth by remember { mutableIntStateOf(0) }
    LaunchedEffect(page) {
        snapshotFlow { stageWidth }.first { it > 0 }
        val move = tween<Float>(240, easing = FastOutSlowInEasing)
        if (page != SettingsPage.Root) {
            shown = page
            shift.snapTo(1f)
            withFrameNanos { }
            withFrameNanos { }
            shift.animateTo(0f, move)
        } else if (shown != SettingsPage.Root) {
            shift.animateTo(1f, move)
            shown = SettingsPage.Root
        }
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val backOwner = LocalLifecycleOwner.current
    DisposableEffect(page, backDispatcher, backOwner) {
        if (page == SettingsPage.Root || backDispatcher == null) {
            return@DisposableEffect onDispose {}
        }
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                pageName = SettingsPage.Root.name
            }
        }
        backDispatcher.addCallback(backOwner, callback)
        onDispose { callback.remove() }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { stageWidth = it.width }
            .appBackground(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = BrandTopInset),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    modifier = Modifier.padding(start = PosterGridInset),
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
            }
            SettingsMenu(
                onOpen = { pageName = it.name },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
        if (shown != SettingsPage.Root) {
            val travel = shift.value
            Column(
                modifier = Modifier
                    .offset { IntOffset((travel * stageWidth).roundToInt(), 0) }
                    .fillMaxSize()
                    .appBackground(),
            ) {
                SettingsBar(
                    title = stringResource(shown.label),
                    onBack = { pageName = SettingsPage.Root.name },
                )
                SettingsLeaf(
                    page = shown,
                    settings = settings,
                    onExtractBackdrop = onExtractBackdrop,
                    onExtractPlex = onExtractPlex,
                    onPlexBackdrop = onPlexBackdrop,
                    onHeroPosterColors = onHeroPosterColors,
                    onHeroPosterPage = onHeroPosterPage,
                    onShowLogos = onShowLogos,
                    onWatchedBadge = onWatchedBadge,
                    onShowPosterTitles = onShowPosterTitles,
                    onWhiteTitles = onWhiteTitles,
                    onHideBottomBarOnScroll = onHideBottomBarOnScroll,
                    onNavLabels = onNavLabels,
                    onSliderShade = onSliderShade,
                    onLargePosters = onLargePosters,
                    onAutoBackdrop = onAutoBackdrop,
                    onWatchGlow = onWatchGlow,
                    onDetailIris = onDetailIris,
                    onHaptic = onHaptic,
                    onAppTheme = onAppTheme,
                    onWatchedPercent = onWatchedPercent,
                    onCountFromStart = onCountFromStart,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                            ),
                        ),
                )
            }
        }
    }
}

@Composable
private fun SettingsLeaf(
    page: SettingsPage,
    settings: UserSettings,
    onExtractBackdrop: (Boolean) -> Unit,
    onExtractPlex: (Boolean) -> Unit,
    onPlexBackdrop: (Boolean) -> Unit,
    onHeroPosterColors: (Boolean) -> Unit,
    onHeroPosterPage: (Boolean) -> Unit,
    onShowLogos: (Boolean) -> Unit,
    onWatchedBadge: (Boolean) -> Unit,
    onShowPosterTitles: (Boolean) -> Unit,
    onWhiteTitles: (Boolean) -> Unit,
    onHideBottomBarOnScroll: (Boolean) -> Unit,
    onNavLabels: (Boolean) -> Unit,
    onSliderShade: (Boolean) -> Unit,
    onLargePosters: (Boolean) -> Unit,
    onAutoBackdrop: (Boolean) -> Unit,
    onWatchGlow: (Boolean) -> Unit,
    onDetailIris: (Boolean) -> Unit,
    onHaptic: (Boolean) -> Unit,
    onAppTheme: (String) -> Unit,
    onWatchedPercent: (Int) -> Unit,
    onCountFromStart: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (page) {
        SettingsPage.Root -> Unit
        SettingsPage.Theme -> Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 18.dp, bottom = 96.dp),
        ) {
            ThemeGroup(selected = settings.appTheme, onSelect = onAppTheme)
        }
        SettingsPage.Posters -> Column(leafScroll(modifier)) {
                SettingGroup(
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.settings_large_posters),
                            hint = stringResource(R.string.settings_large_posters_hint),
                            checked = settings.largePosters,
                            onChecked = onLargePosters,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_slider_shade),
                            hint = stringResource(R.string.settings_slider_shade_hint),
                            checked = settings.sliderShade,
                            onChecked = onSliderShade,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_poster_titles),
                            hint = stringResource(R.string.settings_poster_titles_hint),
                            checked = settings.showPosterTitles,
                            onChecked = onShowPosterTitles,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_white_titles),
                            hint = stringResource(R.string.settings_white_titles_hint),
                            checked = settings.whiteTitles,
                            onChecked = onWhiteTitles,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_watched_badge),
                            hint = stringResource(R.string.settings_watched_badge_hint),
                            checked = settings.watchedBadge,
                            onChecked = onWatchedBadge,
                        ),
                    ),
                )
            }
        SettingsPage.Navigation -> Column(leafScroll(modifier)) {
                SettingGroup(
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.settings_nav_labels),
                            hint = stringResource(R.string.settings_nav_labels_hint),
                            checked = settings.navLabels,
                            onChecked = onNavLabels,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_hide_bar_scroll),
                            hint = stringResource(R.string.settings_hide_bar_scroll_hint),
                            checked = settings.hideBottomBarOnScroll,
                            onChecked = onHideBottomBarOnScroll,
                        ),
                    ),
                )
            }
        SettingsPage.Color -> Column(leafScroll(modifier)) {
                SettingGroup(
                    items = listOfNotNull(
                        SettingItem(
                            title = stringResource(R.string.settings_hero_colors),
                            hint = stringResource(R.string.settings_hero_colors_hint),
                            checked = settings.heroPosterColors,
                            onChecked = onHeroPosterColors,
                        ),
                        if (settings.heroPosterColors) {
                            SettingItem(
                                title = stringResource(R.string.settings_hero_page),
                                hint = stringResource(R.string.settings_hero_page_hint),
                                checked = settings.heroPosterPage,
                                onChecked = onHeroPosterPage,
                            )
                        } else {
                            null
                        },
                        SettingItem(
                            title = stringResource(R.string.settings_extract_plex),
                            hint = stringResource(R.string.settings_extract_plex_hint),
                            checked = settings.extractPlex,
                            onChecked = onExtractPlex,
                        ),
                        if (settings.extractPlex) {
                            SettingItem(
                                title = stringResource(R.string.settings_plex_backdrop),
                                hint = stringResource(R.string.settings_plex_backdrop_hint),
                                checked = settings.plexBackdrop,
                                onChecked = onPlexBackdrop,
                            )
                        } else {
                            null
                        },
                        SettingItem(
                            title = stringResource(R.string.settings_extract_backdrop),
                            hint = stringResource(R.string.settings_extract_backdrop_hint),
                            checked = settings.extractBackdrop,
                            onChecked = onExtractBackdrop,
                        ),
                    ),
                )
            }
        SettingsPage.Detail -> Column(leafScroll(modifier)) {
                SettingGroup(
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.settings_detail_iris),
                            hint = stringResource(R.string.settings_detail_iris_hint),
                            checked = settings.detailIris,
                            onChecked = onDetailIris,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_logos),
                            hint = stringResource(R.string.settings_logos_hint),
                            checked = settings.showLogos,
                            onChecked = onShowLogos,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_backdrop_auto),
                            hint = stringResource(R.string.settings_backdrop_auto_hint),
                            checked = settings.autoBackdrop,
                            onChecked = onAutoBackdrop,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_watch_glow),
                            hint = stringResource(R.string.settings_watch_glow_hint),
                            checked = settings.watchGlow,
                            onChecked = onWatchGlow,
                        ),
                    ),
                )
            }
        SettingsPage.Player -> Column(leafScroll(modifier)) {
            WatchedPercentControl(
                percent = settings.watchedPercent,
                onSelect = onWatchedPercent,
            )
            Spacer(Modifier.height(22.dp))
            SettingGroup(
                items = listOf(
                    SettingItem(
                        title = stringResource(R.string.settings_count_from_start),
                        hint = stringResource(R.string.settings_count_from_start_hint),
                        checked = settings.countFromStart,
                        onChecked = onCountFromStart,
                    ),
                ),
            )
        }
        SettingsPage.Sensors -> Column(leafScroll(modifier)) {
                SettingGroup(
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.settings_haptic),
                            hint = stringResource(R.string.settings_haptic_hint),
                            checked = settings.haptic,
                            onChecked = onHaptic,
                            armHaptic = true,
                        ),
                    ),
                )
            }
        SettingsPage.About -> AboutPage(modifier.padding(top = 18.dp))
    }
}

@Composable
private fun leafScroll(modifier: Modifier): Modifier =
    modifier
        .verticalScroll(rememberScrollState())
        .padding(top = 18.dp, bottom = 96.dp)

@Composable
private fun AboutPage(modifier: Modifier = Modifier) {
    var featuresOpen by rememberSaveable { mutableStateOf(false) }
    val updates = LocalAppUpdate.current
    val context = LocalContext.current
    val tick = rememberHapticTick()
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.amigoplay_logo),
            contentDescription = "AmigoPlay",
            modifier = Modifier
                .padding(start = PosterGridInset - BrandLogoBleed, end = PosterGridInset)
                .height(36.dp)
                .aspectRatio(754f / 186f, matchHeightConstraintsFirst = true),
            alignment = Alignment.CenterStart,
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_version_current),
            modifier = Modifier.padding(horizontal = PosterGridInset),
            color = CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PosterGridInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = BuildConfig.VERSION_NAME,
                color = VersionBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.6).sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.settings_about_description),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        featuresOpen = true
                    },
                ),
                color = VersionBlue,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        }
        val offer = updates.release
        if (offer != null) {
            Spacer(Modifier.height(16.dp))
            UpdateBanner(
                release = offer,
                phase = updates.phase,
                progress = updates.progress,
                onClick = {
                    tick?.invoke()
                    updates.download(context)
                },
            )
        }
        Row(
            modifier = Modifier
                .padding(start = PosterGridInset, end = PosterGridInset, top = if (offer != null) 14.dp else 18.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !updates.checking,
                    onClick = {
                        tick?.invoke()
                        updates.refresh(present = true)
                    },
                )
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.update_check),
                color = Color.Black,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (updates.checking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.Black,
                        strokeWidth = 1.6.dp,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = PosterGridInset, end = PosterGridInset, top = 8.dp)
                .height(18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            val noticeText = when (updates.notice) {
                UpdateNotice.Current -> stringResource(R.string.update_current_notice)
                UpdateNotice.Failed -> stringResource(R.string.update_check_failed)
                null -> null
            }
            if (noticeText != null) {
                Text(
                    text = noticeText,
                    color = if (updates.notice == UpdateNotice.Failed) {
                        Color(0xFFE7B3B3)
                    } else {
                        CinemaMuted
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
        Changelog.releases.firstOrNull()?.let { release ->
            Text(
                text = release.version,
                modifier = Modifier.padding(horizontal = PosterGridInset),
                color = VersionBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
            )
            if (release.added.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                ChangeLabel(stringResource(R.string.changelog_added))
                release.added.forEach { ChangeLine(it) }
            }
            if (release.fixed.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ChangeLabel(stringResource(R.string.changelog_fixed))
                release.fixed.forEach { ChangeLine(it) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (featuresOpen) {
        FeaturesSheet(onDismiss = { featuresOpen = false })
    }
}

@Composable
private fun FeaturesSheet(onDismiss: () -> Unit) {
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.settings_about_features),
                modifier = Modifier.padding(horizontal = 22.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                Changelog.features.forEachIndexed { index, line ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    Text(
                        text = "— $line",
                        color = ChangeText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateBanner(
    release: AppRelease,
    phase: UpdatePhase,
    progress: DownloadProgress,
    onClick: () -> Unit,
) {
    val fraction by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(120),
        label = "bannerProgress",
    )
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .fillMaxWidth()
            .clip(shape)
            .background(settingsPanel())
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = phase != UpdatePhase.Downloading,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(UpdateAlert),
            )
            Text(
                text = stringResource(R.string.update_available),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
            Text(
                text = release.versionName,
                color = VersionBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
        val hint = when (phase) {
            UpdatePhase.Downloading -> downloadStatus(progress)
            UpdatePhase.Failed -> stringResource(R.string.update_failed)
            UpdatePhase.Ready -> stringResource(R.string.update_available_hint)
        }
        Text(
            text = hint,
            modifier = Modifier.padding(top = 4.dp),
            color = if (phase == UpdatePhase.Failed) Color(0xFFE7B3B3) else CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (phase == UpdatePhase.Downloading) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .background(Color.White),
                )
            }
        }
    }
}

private val VersionBlue = Color(0xFF7EC8FF)
private val ChangeText = Color(0xFFD2D2D8)

@Composable
private fun ChangeLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = PosterGridInset),
        color = CinemaMuted,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
}

@Composable
private fun ChangeLine(text: String) {
    Row(
        modifier = Modifier.padding(start = PosterGridInset, end = PosterGridInset, top = 8.dp),
    ) {
        Text(
            text = "—",
            color = ChangeText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = ChangeText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun SettingsMenu(
    onOpen: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val pending = LocalAppUpdate.current.pending
    val pages = SettingsPage.entries.filter { it != SettingsPage.Root }
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 18.dp, bottom = 96.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = PosterGridInset)
                .clip(RoundedCornerShape(20.dp))
                .background(settingsPanel()),
        ) {
            pages.forEachIndexed { index, page ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 64.dp, end = 16.dp)
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                }
                val hint = if (page == SettingsPage.About) {
                    stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME)
                } else {
                    stringResource(page.hint)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember(page) { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                onOpen(page)
                            },
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, end = 12.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(page.label),
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                letterSpacing = (-0.2).sp,
                            )
                            if (page == SettingsPage.About && pending) {
                                UpdateBadge(Modifier.padding(start = 8.dp))
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = hint,
                            color = CinemaMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = ShellIcons.chevron,
                        contentDescription = null,
                        tint = Color(0xFF8A8A90),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsBar(
    title: String,
    onBack: () -> Unit,
) {
    val tick = rememberHapticTick()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = BrandTopInset)
            .height(36.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = PosterGridInset - 7.dp)
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onBack()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = ShellIcons.back,
                contentDescription = stringResource(R.string.section_back),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Чуть над графитовым фоном, без прежнего #2A2E38. */
private val GraphitePanel = Color(0xFF22262E)

private val WatchedPercentSteps = (50..100 step 5).toList()

private fun stepWatchedPercent(current: Int, delta: Int): Int {
    val nearest = WatchedPercentSteps.minBy { kotlin.math.abs(it - current) }
    val index = WatchedPercentSteps.indexOf(nearest).coerceAtLeast(0)
    return WatchedPercentSteps[(index + delta).coerceIn(WatchedPercentSteps.indices)]
}

@Composable
private fun WatchedPercentControl(
    percent: Int,
    onSelect: (Int) -> Unit,
) {
    val tick = rememberHapticTick()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val graphite = settings.appTheme == THEME_GRAPHITE
    Text(
        text = stringResource(R.string.settings_watched_percent),
        modifier = Modifier.padding(start = PosterGridInset + 4.dp, end = PosterGridInset, bottom = 8.dp),
        color = CinemaMuted,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
    Box(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(settingsPanel()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$percent%",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        ThemeKey(
            glyph = "−",
            raised = graphite,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
            onClick = {
                tick?.invoke()
                onSelect(stepWatchedPercent(percent, -1))
            },
        )
        ThemeKey(
            glyph = "+",
            raised = graphite,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
            onClick = {
                tick?.invoke()
                onSelect(stepWatchedPercent(percent, 1))
            },
        )
    }
    Text(
        text = stringResource(R.string.settings_watched_percent_hint),
        modifier = Modifier.padding(start = PosterGridInset + 4.dp, end = PosterGridInset, top = 8.dp),
        color = CinemaMuted,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

private val ThemeOrder = listOf(THEME_CINEMA, THEME_GRAPHITE, THEME_BLACK)

private fun stepTheme(current: String, delta: Int): String {
    val index = ThemeOrder.indexOf(current).let { if (it < 0) 0 else it }
    return ThemeOrder[(index + delta).mod(ThemeOrder.size)]
}

@Composable
private fun settingsPanel(): Color {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    return if (settings.appTheme == THEME_GRAPHITE) GraphitePanel else CinemaSurface
}

@Composable
private fun ThemeGroup(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val tick = rememberHapticTick()
    val graphite = selected == THEME_GRAPHITE
    val panel = if (graphite) GraphitePanel else Color.White.copy(alpha = 0.08f)
    val label = when (selected) {
        THEME_GRAPHITE -> R.string.settings_theme_graphite
        THEME_BLACK -> R.string.settings_theme_black
        else -> R.string.settings_theme_cinema
    }
    fun apply(delta: Int) {
        tick?.invoke()
        onSelect(stepTheme(selected, delta))
    }
    Box(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(panel),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { apply(1) },
                ),
        )
        Text(
            text = stringResource(label),
            modifier = Modifier.padding(horizontal = 52.dp),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ThemeKey(
            glyph = "−",
            raised = graphite,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
            onClick = { apply(-1) },
        )
        ThemeKey(
            glyph = "+",
            raised = graphite,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
            onClick = { apply(1) },
        )
    }
}

@Composable
private fun ThemeKey(
    glyph: String,
    raised: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (raised) Color.White.copy(alpha = 0.07f)
                else Color.White.copy(alpha = 0.14f),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
private data class SettingItem(
    val title: String,
    val hint: String,
    val checked: Boolean,
    val onChecked: (Boolean) -> Unit,
    val armHaptic: Boolean = false,
)

@Composable
private fun SettingGroup(
    items: List<SettingItem>,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .clip(RoundedCornerShape(20.dp))
            .background(settingsPanel()),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
            }
            SettingRow(
                title = item.title,
                hint = item.hint,
                checked = item.checked,
                onChecked = item.onChecked,
                armHaptic = item.armHaptic,
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    hint: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    armHaptic: Boolean = false,
) {
    val tick = rememberHapticTick()
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (tick != null) {
                        tick()
                    } else if (armHaptic && !checked) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    onChecked(!checked)
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        CinemaSwitch(checked)
    }
}

@Composable
private fun CinemaSwitch(checked: Boolean) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val offTrack = if (settings.appTheme == THEME_GRAPHITE) Color(0xFF2A2F38) else Color(0xFF2C2C30)
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "switch",
    )
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else offTrack)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = travel)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color(0xFF8A8A90)),
        )
    }
}
