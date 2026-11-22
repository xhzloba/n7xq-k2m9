package com.example.movie.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.hypot
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.DefaultHomeRailOrder
import com.example.movie.domain.model.GenreRails
import com.example.movie.domain.model.canMoveHomeRail
import com.example.movie.domain.model.folderRailId
import com.example.movie.domain.model.isFolderRail
import com.example.movie.domain.model.moveHomeRail
import com.example.movie.domain.model.syncFolderRails
import com.example.movie.presentation.folders.FolderGlyph
import com.example.movie.presentation.common.BookmarkFilled
import com.example.movie.presentation.common.PlexPlay
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.HomeContentInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue

/** Id полок главной (главный слайдер не входит — всегда на экране). */
internal object HomeRails {
    const val Recommendations = "recommendations"
    const val History = "history"
    const val Continue = "continue"
    const val Favorites = "favorites"
    const val Watched = "watched"
    const val Watching = "watching"
    const val Best = "best"

    data class Entry(
        val id: String,
        val titleRes: Int,
        val icon: ImageVector? = null,
    )

    val Genres: List<Entry> = listOf(
        Entry(GenreRails.Biography, R.string.section_biography),
        Entry(GenreRails.Action, R.string.section_action),
        Entry(GenreRails.Western, R.string.section_western),
        Entry(GenreRails.War, R.string.section_war),
        Entry(GenreRails.Detective, R.string.section_detective),
        Entry(GenreRails.Drama, R.string.section_drama),
        Entry(GenreRails.Historical, R.string.section_historical),
        Entry(GenreRails.Comedy, R.string.section_comedy),
        Entry(GenreRails.Crime, R.string.section_crime),
        Entry(GenreRails.Melodrama, R.string.section_melodrama),
        Entry(GenreRails.Music, R.string.section_music),
        Entry(GenreRails.Musical, R.string.section_musical),
        Entry(GenreRails.Adventure, R.string.section_adventure),
        Entry(GenreRails.Family, R.string.section_family),
        Entry(GenreRails.Sport, R.string.section_sport),
        Entry(GenreRails.Thriller, R.string.section_thriller),
        Entry(GenreRails.Horror, R.string.section_horror),
        Entry(GenreRails.Scifi, R.string.section_scifi),
        Entry(GenreRails.Fantasy, R.string.section_fantasy),
    )

    val Own: List<Entry> = listOf(
        Entry(Recommendations, R.string.section_recommendations),
        Entry(History, R.string.section_history, ShellIcons.history),
        Entry(Continue, R.string.section_continue, PlexPlay),
        Entry(Favorites, R.string.section_favorites, BookmarkFilled),
        Entry(Watched, R.string.section_watched),
        Entry(Watching, R.string.section_watching),
        Entry(Best, R.string.section_best),
    )

    val All: List<Entry> = Genres + Own
}

internal enum class RailsLookTab {
    Look,
    Shelves,
}

private val PlaqueShape = RoundedCornerShape(18.dp)
/** Как поверхности cinema: лёгкая прозрачность. На графите раньше была глухая #2A2E38. */
private val CinemaPlaque = Color.White.copy(alpha = 0.07f)
private val GraphitePlaque = Color(0xFF22262E).copy(alpha = 0.42f)
private val GraphitePanel = Color(0xFF22262E)
private val IconWellCinema = Color.White.copy(alpha = 0.10f)
private val IconWellGraphite = Color.White.copy(alpha = 0.07f)

private fun themePanel(theme: String): Color =
    if (theme == THEME_GRAPHITE) GraphitePanel else Color.White.copy(alpha = 0.08f)

@Composable
internal fun HomeRailsLookPlaque(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    // UltraBlur на всю главную = цветной wash; графитовая плашка там чужая — как cinema.
    val washPage = settings.heroPosterColors && settings.heroPosterPage
    val graphite = settings.appTheme == THEME_GRAPHITE && !washPage
    val plaque = if (graphite) GraphitePlaque else CinemaPlaque
    val iconWell = if (graphite) IconWellGraphite else IconWellCinema
    val tick = rememberHapticTick()
    var pressed by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    val scale = rememberPressScale(pressed)
    val wave = remember { PosterWave() }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PosterGridInset)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(PlaqueShape)
            .background(plaque)
            .posterWave(wave, light = true)
            .pressToOpen(
                onPressed = { pressed = it },
                onClick = {
                    tick?.invoke()
                    onOpen()
                },
                wave = wave,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconWell),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ShellIcons.settings,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_rails_look_title),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.home_rails_look_hint),
                    color = CinemaMuted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = ShellIcons.chevron,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
    if (!settings.hideRailsLookHint) {
        Text(
            text = stringResource(R.string.home_rails_look_about_link),
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Normal,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .padding(start = PosterGridInset, top = 12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        about = true
                    },
                ),
        )
    }
    }
    if (about) {
        val repository = LocalUserSettings.current
        CoverPopup(onDismiss = { about = false }) {
            Column(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, bottom = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_rails_look_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.home_rails_look_about),
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                RailsToggle(
                    title = stringResource(R.string.home_rails_look_about_hide),
                    hint = stringResource(R.string.home_rails_look_about_hide_hint),
                    checked = settings.hideRailsLookHint,
                    onChecked = repository::setHideRailsLookHint,
                )
            }
        }
    }
}

@Composable
internal fun HomeRailsLookSheet(
    onDismiss: () -> Unit,
    initial: RailsLookTab = RailsLookTab.Look,
) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val panel = themePanel(settings.appTheme)
    var tab by remember(initial) { mutableStateOf(initial) }
    // Чуть выше половины экрана; длинный список крутится внутри.
    val sheetCap = (LocalConfiguration.current.screenHeightDp * 0.58f).dp
    val listCap = sheetCap - 108.dp
    // Оверскролл списка не должен утаскивать шит вниз (скролл главной тоже).
    val keepSheet = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = available

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
        }
    }
    CoverPopup(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetCap)
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.home_rails_look_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
            Spacer(Modifier.height(14.dp))
            RailsLookTabs(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PosterGridInset),
            )
            Spacer(Modifier.height(14.dp))
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) {
                            if (forward) it / 10 else -it / 10
                        }
                    val exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                            if (forward) -it / 12 else it / 12
                        }
                    enter togetherWith exit
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = listCap)
                    .padding(horizontal = PosterGridInset),
                label = "rails-look-tab",
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nestedScroll(keepSheet)
                        .clip(RoundedCornerShape(20.dp))
                        .background(panel)
                        .verticalScroll(rememberScrollState()),
                ) {
                    HomeRailsLookBody(page)
                }
            }
        }
    }
}

@Composable
private fun HomeRailsLookBody(page: RailsLookTab) {
    val repository = LocalUserSettings.current
    val settings by repository.settings.collectAsStateWithLifecycle()
    when (page) {
        RailsLookTab.Look -> {
            RailsToggle(
                title = stringResource(R.string.settings_hero_colors),
                hint = stringResource(R.string.settings_hero_colors_hint),
                checked = settings.heroPosterColors,
                onChecked = repository::setHeroPosterColors,
            )
            if (settings.heroPosterColors) {
                RailsDivider()
                RailsToggle(
                    title = stringResource(R.string.settings_hero_page),
                    hint = stringResource(R.string.settings_hero_page_hint),
                    checked = settings.heroPosterPage,
                    onChecked = repository::setHeroPosterPage,
                )
            }
            RailsDivider()
            RailsToggle(
                title = stringResource(R.string.settings_large_posters),
                hint = stringResource(R.string.settings_large_posters_hint),
                checked = settings.largePosters,
                onChecked = repository::setLargePosters,
            )
            RailsDivider()
            RailsToggle(
                title = stringResource(R.string.settings_slider_shade),
                hint = stringResource(R.string.settings_slider_shade_hint),
                checked = settings.sliderShade,
                onChecked = repository::setSliderShade,
            )
            RailsDivider()
            RailsToggle(
                title = stringResource(R.string.settings_poster_titles),
                hint = stringResource(R.string.settings_poster_titles_hint),
                checked = settings.showPosterTitles,
                onChecked = repository::setShowPosterTitles,
            )
            RailsDivider()
            RailsToggle(
                title = stringResource(R.string.settings_release_date),
                hint = stringResource(R.string.settings_release_date_hint),
                checked = settings.showReleaseDate,
                onChecked = repository::setShowReleaseDate,
            )
            RailsDivider()
            RailsToggle(
                title = stringResource(R.string.settings_white_titles),
                hint = stringResource(R.string.settings_white_titles_hint),
                checked = settings.whiteTitles,
                onChecked = repository::setWhiteTitles,
            )
            RailsDivider()
            RailsToggle(
                title = stringResource(R.string.settings_watched_badge),
                hint = stringResource(R.string.settings_watched_badge_hint),
                checked = settings.watchedBadge,
                onChecked = repository::setWatchedBadge,
            )
        }
        RailsLookTab.Shelves -> {
            Text(
                text = stringResource(R.string.home_rails_shelves_note),
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            val folders by LocalAppContainer.current.folders.items.collectAsStateWithLifecycle()
            val folderIds = folders.map { it.id }
            val order = settings.homeRailOrder.syncFolderRails(folderIds)
            val atDefault = order == DefaultHomeRailOrder.syncFolderRails(folderIds)
            val tick = rememberHapticTick()
            val byFolder = folders.associateBy { it.id }
            val rows = order.mapNotNull { id ->
                if (id.isFolderRail()) {
                    byFolder[id.folderRailId()]?.let { folder -> id to folder }
                } else {
                    HomeRails.All.firstOrNull { it.id == id }?.let { id to null }
                }
            }
            val allOn = rows.isNotEmpty() && rows.all { (id, _) -> settings.homeRailVisible(id) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_rails_reset),
                    color = if (atDefault) CinemaMuted else Color.White,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    textDecoration = if (atDefault) null else TextDecoration.Underline,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !atDefault,
                            onClick = {
                                tick?.invoke()
                                repository.setHomeRailOrder(DefaultHomeRailOrder.syncFolderRails(folderIds))
                            },
                        ),
                )
                if (rows.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            if (allOn) R.string.home_rails_disable_all else R.string.home_rails_enable_all,
                        ),
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                repository.setHomeRailsVisible(rows.map { it.first }, visible = !allOn)
                            },
                        ),
                    )
                }
            }
            RailsDivider()
            rows.forEachIndexed { index, (id, folder) ->
                if (index > 0) RailsDivider()
                val entry = if (folder == null) HomeRails.All.first { it.id == id } else null
                RailOrderRow(
                    title = folder?.name ?: stringResource(entry!!.titleRes),
                    icon = if (folder != null) FolderGlyph else entry?.icon,
                    folder = folder != null,
                    checked = settings.homeRailVisible(id),
                    upEnabled = order.canMoveHomeRail(id, up = true),
                    downEnabled = order.canMoveHomeRail(id, up = false),
                    onUp = { repository.setHomeRailOrder(order.moveHomeRail(id, up = true)) },
                    onDown = { repository.setHomeRailOrder(order.moveHomeRail(id, up = false)) },
                    onChecked = { visible -> repository.setHomeRailVisible(id, visible) },
                )
            }
        }
    }
}

/** Круг как зрачок: схлопывается в точку. [progress] 0 — открыто, 1 — закрыто. */
@Composable
internal fun CornerIris(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (progress <= 0f) return
    Canvas(modifier) {
        val hole = hypot(size.width, size.height) / 2f * (1f - progress)
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, size.width, size.height))
            if (hole > 0.5f) {
                addOval(
                    Rect(
                        center.x - hole,
                        center.y - hole,
                        center.x + hole,
                        center.y + hole,
                    ),
                )
            }
        }
        drawPath(path, color)
    }
}

/** Полосы цвета темы заезжают слева направо с задержкой. [progress] 0 — открыто, 1 — закрыто. */
@Composable
internal fun ShutterBlades(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (progress <= 0f) return
    Canvas(modifier) {
        val blades = 6
        val band = size.height / blades
        val edge = 2.dp.toPx()
        for (index in 0 until blades) {
            val local = (progress * 1.4f - index * 0.055f).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val width = size.width * local
            val top = band * index
            drawRect(color, Offset(0f, top), Size(width, band + 1f))
            if (local in 0.04f..0.96f) {
                val glow = (0.22f * (1f - kotlin.math.abs(local - 0.55f) / 0.55f)).coerceIn(0f, 0.22f)
                drawRect(
                    Color.White.copy(alpha = glow),
                    Offset((width - edge).coerceAtLeast(0f), top),
                    Size(edge, band + 1f),
                )
            }
        }
    }
}

/**
 * Долгий тап по «Главной»: цвет темы схлопывается кругом в центр,
 * по центру оформление и полки одной высоты.
 */
@Composable
internal fun HomeRailsCover(
    visible: Boolean,
    color: Color,
    onAlive: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(RailsLookTab.Look) }
    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            onAlive(true)
            progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        } else if (shown) {
            progress.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
            tab = RailsLookTab.Look
            shown = false
            onAlive(false)
        }
    }
    if (!shown) return
    BackHandler(enabled = visible) { onDismiss() }
    val p = progress.value
    val contentAlpha = ((p - 0.78f) / 0.22f).coerceIn(0f, 1f)
    val veil = color.copy(alpha = 1f)
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        CornerIris(progress = p, color = veil, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    val scale = 0.9f + 0.1f * contentAlpha
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(
                        start = HomeContentInset - BrandLogoBleed,
                        end = HomeContentInset - 9.dp,
                        top = BrandTopInset,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RailsLookTabs(
                        selected = tab,
                        onSelect = { tab = it },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = ShellIcons.close,
                            contentDescription = stringResource(R.string.player_close),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(color),
                ) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            val enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) {
                                    if (forward) it / 10 else -it / 10
                                }
                            val exit = fadeOut(tween(160, easing = FastOutSlowInEasing)) +
                                slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                                    if (forward) -it / 12 else it / 12
                                }
                            (enter togetherWith exit).using(
                                SizeTransform(clip = false) { _, _ -> snap() },
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter,
                        label = "rails-cover-tab",
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = navPad),
                        ) {
                            HomeRailsLookBody(page)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RailsLookTabs(
    selected: RailsLookTab,
    onSelect: (RailsLookTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val panel = themePanel(settings.appTheme)
    val pillSpring = spring<Dp>(
        dampingRatio = 0.86f,
        stiffness = Spring.StiffnessMediumLow,
    )
    BoxWithConstraints(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(panel)
            .padding(3.dp),
    ) {
        val gap = 2.dp
        val pillW = (maxWidth - gap) / RailsLookTab.entries.size
        val pillX by animateDpAsState(
            targetValue = pillW * selected.ordinal + gap * selected.ordinal,
            animationSpec = pillSpring,
            label = "rails-tab-pill",
        )
        Box(
            Modifier
                .offset(x = pillX)
                .width(pillW)
                .fillMaxHeight()
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Color.White.copy(
                        alpha = if (settings.appTheme == THEME_GRAPHITE) 0.08f else 0.14f,
                    ),
                ),
        )
        Row(Modifier.fillMaxSize()) {
            RailsLookTab.entries.forEach { tab ->
                val on = tab == selected
                val label = when (tab) {
                    RailsLookTab.Look -> stringResource(R.string.home_rails_tab_look)
                    RailsLookTab.Shelves -> stringResource(R.string.home_rails_tab_shelves)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember(tab) { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!on) {
                                    tick?.invoke()
                                    onSelect(tab)
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (on) Color.White else Color(0xFF8A8A90),
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RailsDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    )
}

@Composable
private fun RailOrderRow(
    title: String,
    icon: ImageVector?,
    checked: Boolean,
    upEnabled: Boolean,
    downEnabled: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onChecked: (Boolean) -> Unit,
    folder: Boolean = false,
) {
    val tick = rememberHapticTick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RailNudge(up = true, enabled = upEnabled, onClick = {
            tick?.invoke()
            onUp()
        })
        RailNudge(up = false, enabled = downEnabled, onClick = {
            tick?.invoke()
            onDown()
        })
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onChecked(!checked)
                    },
                )
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val ink = if (folder) PlayBlue else Color.White
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = title,
                color = ink,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            RailsSwitch(checked)
        }
    }
}

@Composable
private fun RailNudge(up: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.28f }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = ShellIcons.chevron,
            contentDescription = stringResource(if (up) R.string.home_rails_up else R.string.home_rails_down),
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = if (up) -90f else 90f },
        )
    }
}

@Composable
private fun RailsToggle(
    title: String,
    hint: String? = null,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    icon: ImageVector? = null,
) {
    val tick = rememberHapticTick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onChecked(!checked)
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
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
            if (hint != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = hint,
                    color = CinemaMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
        RailsSwitch(checked)
    }
}

@Composable
private fun RailsSwitch(checked: Boolean) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val offTrack = if (settings.appTheme == THEME_GRAPHITE) Color(0xFF2A2F38) else Color(0xFF2C2C30)
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "rails-switch",
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
