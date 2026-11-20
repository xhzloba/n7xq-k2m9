package com.example.movie.presentation.favorites

import android.graphics.drawable.ColorDrawable
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.movie.presentation.home.PeekEnter
import com.example.movie.presentation.home.PeekExit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.presentation.common.BookmarkFilled
import com.example.movie.presentation.common.PosterImage
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.common.appChrome
import com.example.movie.data.download.OfflineItem
import com.example.movie.data.download.OfflinePhase
import com.example.movie.data.download.OfflineStatus
import com.example.movie.presentation.common.glideDownloadPercent
import com.example.movie.presentation.common.inProgress
import com.example.movie.data.folders.LibraryFolder
import com.example.movie.presentation.folders.FolderCreateSheet
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.common.drawTopScrim
import com.example.movie.presentation.common.scrollToTopOn
import com.example.movie.presentation.common.headerThemeBrush
import com.example.movie.presentation.common.headerFillOn
import com.example.movie.presentation.common.topScrimFade
import com.example.movie.presentation.common.topScrimHeight
import com.example.movie.presentation.common.topScrimPalette
import com.example.movie.presentation.detail.DetailSeed
import com.example.movie.presentation.detail.OfflinePlayer
import com.example.movie.ui.theme.PlayBlue
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.SeriesSub
import com.example.movie.domain.model.progressCaption
import com.example.movie.domain.model.toMovie
import com.example.movie.domain.model.toSaved
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.domain.model.HapticSlot
import com.example.movie.presentation.haptic.rememberHaptic
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.SectionScrim
import com.example.movie.presentation.home.components.RailSeeAllCard
import com.example.movie.presentation.home.components.RailTitle
import com.example.movie.presentation.home.components.RecommendationRail
import com.example.movie.presentation.home.components.posterAspect
import com.example.movie.presentation.home.components.posterSize
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.LogoAspect
import com.example.movie.presentation.home.components.LogoHeight
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.PosterCaption
import com.example.movie.presentation.home.components.PosterGap
import com.example.movie.presentation.home.components.WatchedBadgeHang
import com.example.movie.presentation.home.components.WatchProgress
import com.example.movie.presentation.home.components.posterGridColumns
import com.example.movie.presentation.home.components.posterTitlesOn
import com.example.movie.presentation.home.components.BrandWordmark
import com.example.movie.presentation.home.components.MenuSombreroEffect
import com.example.movie.presentation.home.components.LogoMood
import com.example.movie.presentation.home.components.CategoryTabFontSize
import com.example.movie.presentation.home.components.CategoryTabLetterSpacing
import com.example.movie.presentation.home.components.CategoryTabLineHeight
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface
import com.example.movie.ui.theme.PosterShape

private val FavoritePlay = Color(0xFFE23B5A)
private val CapsuleInk = Color.White.copy(alpha = 0.72f)
private val FilterShape = RoundedCornerShape(22.dp)
private val FilterNotes = Color(0xFFD2D2D8)
private val LibrarySheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
private val LibraryMenuScrim = Color(0xFF3A3A3E).copy(alpha = 0.86f)
private val LibraryRowInk = Color(0xFFE8E8EC)

private enum class LibrarySort(@StringRes val label: Int) {
    Genre(R.string.favorites_sort_genre),
    Year(R.string.favorites_sort_year),
    Title(R.string.favorites_sort_title),
}

private fun decodeFilters(raw: String): Map<String, String> {
    if (raw.isEmpty()) return emptyMap()
    return raw.split('\u001e').mapNotNull { row ->
        val split = row.indexOf('\u001f')
        if (split <= 0) null else row.substring(0, split) to row.substring(split + 1)
    }.toMap()
}

private fun encodeFilters(filters: Map<String, String>): String =
    filters.entries.joinToString("\u001e") { "${it.key}\u001f${it.value}" }

private fun Map<String, String>.with(key: String, value: String?): Map<String, String> {
    val next = toMutableMap()
    if (value == null) next.remove(key) else next[key] = value
    return next
}

private enum class FavoriteTab(@StringRes val label: Int, val type: String?, val hasGenres: Boolean) {
    All(R.string.favorites_all, null, true),
    Movies(R.string.favorites_movies, "movie", true),
    Series(R.string.favorites_series, "serial", true),
    Cartoons(R.string.favorites_cartoons, "multfilm", false),
    Anime(R.string.favorites_anime, "anime", false),
}

@Composable
internal fun FavoritesScreen(
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onPlayResume: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    scrollTop: Int = 0,
) {
    val container = LocalAppContainer.current
    val favorites by container.favorites.items.collectAsStateWithLifecycle()
    val watched by container.watched.items.collectAsStateWithLifecycle()
    val history by container.viewHistory.items.collectAsStateWithLifecycle()
    val resume by container.resume.items.collectAsStateWithLifecycle()
    val alerts by container.releaseWatch.store.subs.collectAsStateWithLifecycle()
    val downloads by container.offline.items.collectAsStateWithLifecycle()
    val downloadTasks by container.offline.tasks.collectAsStateWithLifecycle()
    val downloadPending by container.offline.pending.collectAsStateWithLifecycle()
    var libraryName by rememberSaveable { mutableStateOf("") }
    var playing by remember { mutableStateOf<OfflinePlay?>(null) }
    var openedDownload by rememberSaveable { mutableStateOf<String?>(null) }
    val notices = LocalShelfNotices.current
    LaunchedEffect(notices.openRequest) {
        if (notices.openRequest == 0) return@LaunchedEffect
        if (LibrarySection.entries.any { it.name == notices.section }) libraryName = notices.section
    }
    val library = LibrarySection.entries.firstOrNull { it.name == libraryName }
    val folders by container.folders.items.collectAsStateWithLifecycle()
    var foldersOpen by rememberSaveable { mutableStateOf(false) }
    var openedFolder by rememberSaveable { mutableIntStateOf(-1) }
    var editingFolder by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = library != null || foldersOpen || openedFolder >= 0) {
        openedDownload = null
        when {
            openedFolder >= 0 -> openedFolder = -1
            library != null -> libraryName = ""
            else -> foldersOpen = false
        }
    }
    val items = when (library) {
        null -> emptyList()
        LibrarySection.Favorites -> favorites
        LibrarySection.Continue -> resume.map { it.toSaved() }
        LibrarySection.History -> history
        LibrarySection.Watched -> watched
        LibrarySection.Alerts -> alerts.map { it.toLibrary() }
        LibrarySection.Downloads -> downloads.map { it.title }
        LibrarySection.Actors -> emptyList()
    }
    val downloadStatus = { item: OfflineItem ->
        container.offline.status(item, downloadTasks, downloadPending)
    }
    val opened = downloads.firstOrNull { it.title.id == openedDownload }
    var tabName by rememberSaveable { mutableStateOf(FavoriteTab.All.name) }
    var genreRaw by rememberSaveable { mutableStateOf("") }
    var sortRaw by rememberSaveable { mutableStateOf("") }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    val logo = remember { LogoMood() }
    val tab = FavoriteTab.valueOf(tabName)
    val genre = decodeFilters(genreRaw)[tab.name]
    val sort = decodeFilters(sortRaw)[tab.name]?.let { runCatching { LibrarySort.valueOf(it) }.getOrNull() }
    val inTab = tab.type?.let { type -> items.filter { it.type == type } } ?: items
    val genres = if (tab.hasGenres) inTab.genreLabels() else emptyList()
    val filtered = genre?.let { name -> inTab.filter { name in it.genreKeys() } } ?: inTab
    val shown = filtered.sortedFor(sort)
    val gridState = rememberLazyGridState()
    gridState.scrollToTopOn(scrollTop)
    val closePage = {
        openedDownload = null
        libraryName = ""
    }
    val onSelect = { next: FavoriteTab ->
        filtersOpen = false
        tabName = next.name
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (library == null && openedFolder in folders.indices) {
            FolderInside(
                folder = folders[openedFolder],
                onBack = { openedFolder = -1 },
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
            )
        } else if (library == null && foldersOpen) {
            FolderPage(
                folders = folders,
                onBack = { foldersOpen = false },
                onAdd = { editingFolder = -1 },
                onOpen = { openedFolder = it },
                onEdit = { editingFolder = it },
                scrollTop = scrollTop,
            )
        } else if (library == null) {
            LibraryHub(
                favorites = favorites,
                resume = resume,
                history = history,
                watched = watched,
                downloads = downloads,
                onMovieClick = onMovieClick,
                onMoviePeek = onMoviePeek,
                onPlayResume = onPlayResume,
                onBack = onBack,
                onOpenCatalog = onOpenCatalog,
                onOpen = { libraryName = it.name },
                onOpenDownload = { item ->
                    if (item.streams.any { it.season > 0 || it.episode > 0 }) {
                        openedDownload = item.title.id
                    } else {
                        playing = OfflinePlay(item, null, null)
                    }
                },
                folders = folders,
                downloadStatus = downloadStatus,
                onOpenFolders = { foldersOpen = true },
                onOpenFolder = { openedFolder = it },
                onEditFolder = { editingFolder = it },
                onAddFolder = { editingFolder = -1 },
                scrollTop = scrollTop,
            )
        } else if (library == LibrarySection.Downloads) {
            val listState = rememberLazyListState()
            val header = libraryTopPadding()
            Box(Modifier.fillMaxSize()) {
                DownloadsLibrary(
                    items = downloads,
                    topPadding = header,
                    listState = listState,
                    statusOf = downloadStatus,
                    onOpen = { openedDownload = it.title.id },
                    onPlay = { playing = OfflinePlay(it, null, null) },
                    onPause = { container.offline.pause(it.title.id) },
                    onResume = { container.offline.resume(it.title.id) },
                    onDelete = { container.offline.remove(it.title.id) },
                )
                SectionScrim(
                    progress = listState.libraryScrim(),
                    modifier = Modifier.align(Alignment.TopCenter),
                )
                LibraryBar(
                    title = stringResource(R.string.library_downloads),
                    onBack = closePage,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        } else if (shown.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(top = libraryTopPadding())) {
                FavoriteTabs(
                    selected = tab,
                    genres = genres,
                    genre = genre,
                    filtersOpen = filtersOpen,
                    onSelect = onSelect,
                    onOpenFilters = { filtersOpen = true },
                    onMenuSettled = logo::onSettled,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 96.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (library) {
                                LibrarySection.Alerts -> R.string.library_alerts_empty
                                LibrarySection.Downloads -> R.string.downloads_empty
                                else -> R.string.catalog_empty
                            },
                        ),
                        modifier = Modifier.padding(horizontal = 32.dp),
                        color = CinemaMuted,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                }
            }
            LibraryBar(
                title = stringResource(library.label),
                onBack = closePage,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        } else {
            Box(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(posterGridColumns()),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = PosterGridInset,
                    end = PosterGridInset,
                    top = libraryTopPadding(),
                    bottom = 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(PosterGap),
                verticalArrangement = Arrangement.spacedBy(PosterGap),
            ) {
                item(key = "tabs", span = { GridItemSpan(maxLineSpan) }) {
                    FavoriteTabs(
                        selected = tab,
                        genres = genres,
                        genre = genre,
                        filtersOpen = filtersOpen,
                        onSelect = onSelect,
                        onOpenFilters = { filtersOpen = true },
                        onMenuSettled = logo::onSettled,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .layout { measurable, constraints ->
                                val bleed = PosterGridInset.roundToPx()
                                val placeable = measurable.measure(
                                    constraints.copy(maxWidth = constraints.maxWidth + bleed * 2),
                                )
                                layout(constraints.maxWidth, placeable.height) {
                                    placeable.place(-bleed, 0)
                                }
                            },
                    )
                }
                items(shown, key = { it.id }) { item ->
                    val mark = if (library == LibrarySection.Continue) resume.firstOrNull { it.id == item.id } else null
                    FavoritePoster(
                        item = item,
                        progress = mark?.fraction,
                        caption = mark?.progressCaption(),
                        // В «Продолжить» тап открывает то же меню, что долгий тап — play и «Удалить просмотр».
                        onMovieClick = if (library == LibrarySection.Continue) {
                            { onMoviePeek(item.toMovie()) }
                        } else {
                            onMovieClick
                        },
                        onMoviePeek = onMoviePeek,
                    )
                }
            }
            SectionScrim(
                progress = gridState.libraryScrim(),
                modifier = Modifier.align(Alignment.TopCenter),
            )
            LibraryBar(
                title = stringResource(library.label),
                onBack = closePage,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            }
        }
        if (opened != null) {
            DownloadedEpisodes(
                item = opened,
                statusOf = downloadStatus,
                onPlay = { season, episode -> playing = OfflinePlay(opened, season, episode) },
                onRemove = { season, episode ->
                    container.offline.removeStream(opened.title.id, season, episode)
                },
                onPause = { season, episode ->
                    container.offline.pauseStream(opened.title.id, season, episode)
                },
                onResume = { season, episode ->
                    container.offline.resumeStream(opened.title.id, season, episode)
                },
                onBack = { openedDownload = null },
            )
        }
        LibraryFilterDialog(
                open = filtersOpen && tab.hasGenres && library != null && library != LibrarySection.Downloads,
                title = stringResource(tab.label),
                genres = genres,
                genre = genre,
                sort = sort,
                onGenre = { next ->
                    genreRaw = encodeFilters(decodeFilters(genreRaw).with(tab.name, next))
                },
                onSort = { next ->
                    val value = if (sort == next) null else next.name
                    sortRaw = encodeFilters(decodeFilters(sortRaw).with(tab.name, value))
                },
                onDismiss = { filtersOpen = false },
            )
        playing?.let { play ->
            OfflinePlayer(
                item = play.item,
                season = play.season,
                episode = play.episode,
                onDismiss = { playing = null },
            )
        }
        val editor = editingFolder
        if (editor != null && editor in folders.indices) {
            FolderNameDialog(
                initial = folders[editor],
                onDismiss = { editingFolder = null },
                onSave = { name, about ->
                    container.folders.replace(
                        folders.toMutableList().also {
                            it[editor] = folders[editor].copy(name = name, description = about)
                        },
                    )
                    editingFolder = null
                },
            )
        } else if (editor != null) {
            FolderCreateSheet(
                onDismiss = { editingFolder = null },
                onCreate = { name, about ->
                    container.folders.add(name, about, null)
                    editingFolder = null
                },
            )
        }
    }
}

private const val LibraryRailCap = 15

@Composable
private fun LibraryHub(
    favorites: List<SavedTitle>,
    resume: List<ResumeItem>,
    history: List<SavedTitle>,
    watched: List<SavedTitle>,
    downloads: List<OfflineItem>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onPlayResume: (String) -> Unit,
    onBack: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpen: (LibrarySection) -> Unit,
    onOpenDownload: (OfflineItem) -> Unit,
    downloadStatus: (OfflineItem) -> OfflineStatus,
    folders: List<LibraryFolder>,
    onOpenFolders: () -> Unit,
    onOpenFolder: (Int) -> Unit,
    onEditFolder: (Int) -> Unit,
    onAddFolder: () -> Unit,
    scrollTop: Int,
) {
    val scroll = rememberScrollState()
    scroll.scrollToTopOn(scrollTop)
    val header = libraryTopPadding()
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = header, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            if (favorites.isEmpty()) {
                LibraryPlaque(
                    title = stringResource(R.string.section_favorites),
                    body = stringResource(R.string.library_watch_empty),
                    icon = BookmarkFilled,
                    iconSize = 92.dp,
                    onClick = onOpenCatalog,
                )
            } else {
                LibraryRail(
                    title = stringResource(R.string.section_favorites),
                    movies = favorites.take(LibraryRailCap).map { it.toMovie() },
                    onMovieClick = onMovieClick,
                    onMoviePeek = onMoviePeek,
                    onOpen = { onOpen(LibrarySection.Favorites) },
                )
            }
            if (resume.isNotEmpty()) {
                val rail = resume.take(LibraryRailCap)
                LibraryRail(
                    title = stringResource(R.string.section_continue),
                    movies = rail.map { it.toMovie() },
                    onMovieClick = onPlayResume,
                    onMoviePeek = onMoviePeek,
                    onOpen = { onOpen(LibrarySection.Continue) },
                    progressOf = { movie -> rail.firstOrNull { it.id == movie.id }?.fraction },
                    captionOf = { movie -> rail.firstOrNull { it.id == movie.id }?.progressCaption() },
                )
            }
            if (history.isNotEmpty()) {
                LibraryRail(
                    title = stringResource(R.string.section_history),
                    movies = history.take(LibraryRailCap).map { it.toMovie() },
                    onMovieClick = onMovieClick,
                    onMoviePeek = onMoviePeek,
                    onOpen = { onOpen(LibrarySection.History) },
                )
            }
            if (watched.isEmpty()) {
                LibraryPlaque(
                    title = stringResource(R.string.library_watched),
                    body = stringResource(R.string.library_watched_empty),
                    icon = Icons.Filled.CheckCircle,
                    iconSize = 92.dp,
                    onClick = onOpenCatalog,
                )
            } else {
                LibraryRail(
                    title = stringResource(R.string.library_watched),
                    movies = watched.take(LibraryRailCap).map { it.toMovie() },
                    onMovieClick = onMovieClick,
                    onMoviePeek = onMoviePeek,
                    onOpen = { onOpen(LibrarySection.Watched) },
                )
            }
            if (downloads.isEmpty()) {
                LibraryPlaque(
                    title = stringResource(R.string.library_downloads),
                    body = stringResource(R.string.library_downloads_empty),
                    action = stringResource(R.string.library_downloads_pick),
                    onClick = onOpenCatalog,
                )
            } else {
                LibraryRail(
                    title = stringResource(R.string.library_downloads),
                    movies = downloads.take(LibraryRailCap).map { it.title.toMovie() },
                    onMovieClick = { id ->
                        downloads.firstOrNull { it.title.id == id }?.let(onOpenDownload)
                    },
                    onMoviePeek = onMoviePeek,
                    onOpen = { onOpen(LibrarySection.Downloads) },
                    titleShimmer = downloads.any { item ->
                        val phase = downloadStatus(item).phase
                        phase == OfflinePhase.Downloading || phase == OfflinePhase.Preparing
                    },
                    overlay = { movie ->
                        val item = downloads.firstOrNull { it.title.id == movie.id } ?: return@LibraryRail
                        val status = downloadStatus(item)
                        if (status.phase.inProgress()) DownloadRailMark(status)
                    },
                )
            }
            if (folders.isEmpty()) {
                LibraryPlaque(
                    title = stringResource(R.string.library_folders),
                    icon = FolderMark,
                    iconSize = 52.dp,
                    onAdd = onAddFolder,
                )
            } else {
                FolderRail(
                    folders = folders,
                    onOpen = onOpenFolders,
                    onOpenFolder = onOpenFolder,
                    onEditFolder = onEditFolder,
                    onAdd = onAddFolder,
                )
            }
        }
        SectionScrim(
            progress = scroll.libraryScrim(),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        LibraryBar(
            title = stringResource(R.string.nav_favorites),
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun LibraryBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    mark: ImageVector? = null,
) {
    val tick = rememberHapticTick()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = BrandTopInset)
            .height(36.dp),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mark != null) {
                Icon(
                    imageVector = mark,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
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

@Composable
private fun LibraryRail(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onOpen: () -> Unit,
    progressOf: (Movie) -> Float? = { null },
    captionOf: (Movie) -> String? = { null },
    overlay: @Composable (Movie) -> Unit = {},
    titleShimmer: Boolean = false,
) {
    RecommendationRail(
        movies = movies,
        onMovieClick = onMovieClick,
        onMoviePeek = onMoviePeek,
        title = title,
        onOpen = onOpen,
        stickToStart = true,
        progressOf = progressOf,
        captionOf = captionOf,
        overlay = overlay,
        titleShimmer = titleShimmer,
    )
}

@Composable
private fun DownloadRailMark(status: OfflineStatus) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                color = PlayBlue,
                strokeWidth = 1.6.dp,
                modifier = Modifier.size(14.dp),
            )
            if (!status.indeterminate) {
                DownloadPercent(status.percent)
            }
        }
    }
}

@Composable
private fun DownloadPercent(percent: Int) {
    val shown = glideDownloadPercent(percent)
    var width by remember { mutableIntStateOf(0) }
    val shift by rememberInfiniteTransition(label = "download-percent").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1400, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "download-percent-shift",
    )
    val span = width.toFloat().coerceAtLeast(1f)
    val band = span * 0.7f
    val x = -band + (span + band * 2f) * shift
    Text(
        text = "$shown%",
        modifier = Modifier.padding(start = 6.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        onTextLayout = { width = it.size.width },
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    Color.White,
                    Color.White.copy(alpha = 0.4f),
                ),
                start = Offset(x, 0f),
                end = Offset(x + band, 0f),
            ),
        ),
    )
}

@Composable
private fun libraryTopPadding(): Dp {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    return top + BrandTopInset + 36.dp + 18.dp
}

@Composable
private fun ScrollState.libraryScrim(): Float {
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    return (value / threshold).coerceIn(0f, 1f)
}

@Composable
private fun LazyListState.libraryScrim(): Float {
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    return if (firstVisibleItemIndex > 0) 1f else (firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
}

@Composable
private fun LazyGridState.libraryScrim(): Float {
    val threshold = with(LocalDensity.current) { 28.dp.toPx() }
    return if (firstVisibleItemIndex > 0) 1f else (firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
}

@Composable
private fun LibraryPlaque(
    title: String,
    body: String? = null,
    icon: ImageVector? = null,
    iconSize: Dp = 72.dp,
    action: String? = null,
    onClick: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.06f))
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                },
            ),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.14f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
                    .size(iconSize),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
            )
            if (body != null) {
                Text(
                    text = body,
                    modifier = Modifier.padding(top = 8.dp, end = if (icon != null) 72.dp else 0.dp),
                    color = CinemaMuted,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                )
            }
            if (action != null) {
                Text(
                    text = action,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = 0.14f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            if (onAdd != null) {
                FolderAdd(onClick = onAdd, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
private fun FolderAdd(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val tick = rememberHapticTick()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.08f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.library_folder_new),
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
private fun FolderTitlePlus(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tick = rememberHapticTick()
    Row(
        modifier
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onClick()
                },
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.library_folder_new),
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun FolderRail(
    folders: List<LibraryFolder>,
    onOpen: () -> Unit,
    onOpenFolder: (Int) -> Unit,
    onEditFolder: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val width = 228.dp
    val height = 124.dp
    val shown = folders.take(LibraryRailCap)
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RailTitle(
                title = stringResource(R.string.library_folders),
                onClick = onOpen,
                modifier = Modifier.weight(1f),
            )
            FolderTitlePlus(
                onClick = onAdd,
                modifier = Modifier.padding(end = PosterGridInset),
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(start = PosterGridInset, end = PosterGridInset),
            horizontalArrangement = Arrangement.spacedBy(PosterGap),
        ) {
            itemsIndexed(shown, key = { index, folder -> "$index:${folder.name}" }) { index, folder ->
                FolderCard(
                    folder = folder,
                    modifier = Modifier
                        .width(width)
                        .height(height),
                    onClick = { onOpenFolder(index) },
                    onEdit = { onEditFolder(index) },
                )
            }
            item(key = "folders-see-all") {
                RailSeeAllCard(
                    width = 132.dp,
                    height = height,
                    glass = false,
                    onClick = onOpen,
                )
            }
        }
    }
}

@Composable
private fun FolderCard(
    folder: LibraryFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    val tick = rememberHapticTick()
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember(folder.name) { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onClick()
                    },
                )
                .padding(start = 14.dp, end = if (onEdit != null) 36.dp else 14.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Icon(
                imageVector = FolderMark,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.28f),
                modifier = Modifier.size(26.dp),
            )
            Text(
                text = folder.name,
                modifier = Modifier.padding(top = 8.dp),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (folder.description.isNotBlank()) {
                Text(
                    text = folder.description,
                    modifier = Modifier.padding(top = 4.dp),
                    color = CinemaMuted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onEdit != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onEdit()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.library_folder_edit),
                    tint = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun FolderPage(
    folders: List<LibraryFolder>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    scrollTop: Int,
) {
    val scroll = rememberScrollState()
    scroll.scrollToTopOn(scrollTop)
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = libraryTopPadding(), bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            folders.indices.toList().chunked(2).forEach { indexes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PosterGridInset),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    indexes.forEach { index ->
                        FolderCard(
                            folder = folders[index],
                            onClick = { onOpen(index) },
                            onEdit = { onEdit(index) },
                            modifier = Modifier
                                .weight(1f)
                                .height(118.dp),
                        )
                    }
                    if (indexes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        SectionScrim(
            progress = scroll.libraryScrim(),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        LibraryBar(
            title = stringResource(R.string.library_folders),
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        FolderAdd(
            onClick = onAdd,
            size = 40.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = BrandTopInset, end = PosterGridInset),
        )
    }
}

@Composable
private fun FolderInside(
    folder: LibraryFolder,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (folder.titles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = libraryTopPadding(), bottom = 96.dp)
                    .padding(horizontal = PosterGridInset),
            ) {
                if (folder.description.isNotBlank()) {
                    Text(
                        text = folder.description,
                        color = CinemaMuted,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.library_folder_empty),
                        color = CinemaMuted,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(posterGridColumns()),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = PosterGridInset,
                    end = PosterGridInset,
                    top = libraryTopPadding(),
                    bottom = 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(PosterGap),
                verticalArrangement = Arrangement.spacedBy(PosterGap),
            ) {
                if (folder.description.isNotBlank()) {
                    item(key = "about", span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = folder.description,
                            color = CinemaMuted,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
                items(folder.titles, key = { it.id }) { item ->
                    FavoritePoster(
                        item = item,
                        onMovieClick = onMovieClick,
                        onMoviePeek = onMoviePeek,
                    )
                }
            }
            SectionScrim(
                progress = gridState.libraryScrim(),
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
        LibraryBar(
            title = folder.name,
            onBack = onBack,
            mark = FolderMark,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

private val FolderDialogShape = RoundedCornerShape(18.dp)

@Composable
private fun FolderNameDialog(
    initial: LibraryFolder?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var about by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    val ready = name.isNotBlank()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setDimAmount(0f)
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 36.dp)
                    .fillMaxWidth()
                    .clip(FolderDialogShape)
                    .background(CinemaSurface)
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), FolderDialogShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(if (initial == null) R.string.library_folder_new else R.string.library_folder_edit),
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                )
                FolderField(
                    value = name,
                    onValueChange = { name = it.replace('\n', ' ') },
                    label = stringResource(R.string.library_folder_name),
                    singleLine = true,
                    modifier = Modifier.padding(top = 16.dp),
                )
                FolderField(
                    value = about,
                    onValueChange = { about = it },
                    label = stringResource(R.string.library_folder_about),
                    singleLine = false,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.library_folder_cancel),
                        color = CinemaMuted,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Text(
                        text = stringResource(if (initial == null) R.string.library_folder_create else R.string.library_folder_save),
                        color = Color.White.copy(alpha = if (ready) 1f else 0.28f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = ready,
                                onClick = { onSave(name.trim(), about.trim()) },
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = label,
            color = CinemaMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 20.sp,
            ),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .heightIn(min = if (singleLine) 40.dp else 72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                    inner()
                }
            },
        )
    }
}

private val FolderMark: ImageVector = ImageVector.Builder(
    name = "folder",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(fill = SolidColor(Color.Black)) {
    moveTo(10f, 4f)
    lineTo(12f, 6f)
    horizontalLineTo(20f)
    curveTo(21.1f, 6f, 22f, 6.9f, 22f, 8f)
    verticalLineTo(18f)
    curveTo(22f, 19.1f, 21.1f, 20f, 20f, 20f)
    horizontalLineTo(4f)
    curveTo(2.9f, 20f, 2f, 19.1f, 2f, 18f)
    verticalLineTo(6f)
    curveTo(2f, 4.9f, 2.9f, 4f, 4f, 4f)
    horizontalLineTo(10f)
    close()
}.build()

@Composable
private fun LibraryBrand(
    hat: Boolean,
    mustache: Boolean,
    library: LibrarySection,
    onLibrary: (LibrarySection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = PosterGridInset - BrandLogoBleed, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandWordmark(play = FavoritePlay, hat = hat, mustache = mustache)
        Spacer(Modifier.weight(1f))
        LibraryMenu(selected = library, onSelect = onLibrary)
    }
}

@Composable
private fun LibraryScrim(progress: Float, modifier: Modifier = Modifier) {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val palette = topScrimPalette()
    val fill = headerFillOn()
    val plate = headerThemeBrush()
    Box(
        modifier
            .fillMaxWidth()
            .height(topScrimHeight(top))
            .topScrimFade(progress)
            .drawBehind {
                drawTopScrim(fill, plate, null, palette) {
                val y = top.toPx() + BrandTopInset.toPx() + 20.dp.toPx()
                val radius = 140.dp.toPx()
                val logoWidth = LogoHeight.toPx() * LogoAspect
                val logoX = (PosterGridInset - BrandLogoBleed).toPx() + logoWidth / 2f
                val menuX = size.width - (14.dp.toPx() + 20.dp.toPx())
                fun pool(center: Offset) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0f to palette.poolCore,
                                0.45f to palette.poolMid,
                                1f to Color.Transparent,
                            ),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
                pool(Offset(logoX, y))
                pool(Offset(menuX, y))
                }
            },
    )
}

private enum class LibrarySection(@StringRes val label: Int) {
    Favorites(R.string.section_favorites),
    Continue(R.string.section_continue),
    History(R.string.section_history),
    Watched(R.string.library_watched),
    Downloads(R.string.favorites_downloaded),
    Alerts(R.string.library_alerts),
    Actors(R.string.favorites_actors),
}

private data class OfflinePlay(val item: OfflineItem, val season: Int?, val episode: Int?)

private fun SeriesSub.toLibrary(): SavedTitle = SavedTitle(
    id = id,
    title = title,
    posterUrl = posterUrl,
    year = year,
    type = type.ifBlank { "serial" },
)

@Composable
private fun LibraryMenu(
    selected: LibrarySection,
    onSelect: (LibrarySection) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val tick = rememberHapticTick()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(CinemaSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    open = true
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        MenuArrow(
            open = open,
            tint = CapsuleInk,
            size = 22.dp,
        )
    }
    if (open) {
        LibraryPickerSheet(
            selected = selected,
            onSelect = { section ->
                onSelect(section)
                open = false
            },
            onDismiss = { open = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryPickerSheet(
    selected: LibrarySection,
    onSelect: (LibrarySection) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tick = rememberHapticTick()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = appChrome(),
        contentColor = Color.White,
        scrimColor = LibraryMenuScrim,
        dragHandle = null,
        shape = LibrarySheetShape,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appBackground()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.28f)),
            )
            Text(
                text = stringResource(R.string.nav_favorites),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 6.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.3).sp,
                textAlign = TextAlign.Center,
            )
            LibrarySection.entries.forEach { section ->
                val lit = section == selected
                Text(
                    text = stringResource(section.label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember(section.name) { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                onSelect(section)
                            },
                        )
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    color = if (lit) PlayBlue else LibraryRowInk,
                    fontWeight = if (lit) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun List<SavedTitle>.genreLabels(): List<String> {
    val labels = LinkedHashMap<String, String>()
    for (item in this) {
        item.genres.split(',').forEach { raw ->
            val label = raw.trim()
            if (label.isEmpty()) return@forEach
            labels.putIfAbsent(label.lowercase(), label)
        }
    }
    return labels.values.toList()
}

private fun SavedTitle.genreKeys(): Set<String> =
    genres.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

private fun List<SavedTitle>.sortedFor(sort: LibrarySort?): List<SavedTitle> = when (sort) {
    null -> this
    LibrarySort.Title -> sortedBy { it.title.lowercase() }
    LibrarySort.Year -> sortedWith(
        compareByDescending<SavedTitle> { it.year != null }
            .thenByDescending { it.year ?: 0 }
            .thenBy { it.title.lowercase() },
    )
    LibrarySort.Genre -> sortedWith(
        compareBy<SavedTitle> { it.genres.substringBefore(',').trim().lowercase() }
            .thenBy { it.title.lowercase() },
    )
}

@Composable
private fun FavoriteTabs(
    selected: FavoriteTab,
    genres: List<String>,
    genre: String?,
    filtersOpen: Boolean,
    onSelect: (FavoriteTab) -> Unit,
    onOpenFilters: () -> Unit,
    onMenuSettled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    MenuSombreroEffect(listState, onMenuSettled)
    HapticOnChange(listState) { listState.passedTab() }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PosterGridInset),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(FavoriteTab.entries, key = { it.name }) { tab ->
            val lit = tab == selected
            val title = stringResource(tab.label)
            val picked = genres.firstOrNull { it.lowercase() == genre }
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember(tab.name) { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (tab != selected) onSelect(tab)
                        else if (tab.hasGenres) onOpenFilters()
                    },
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildAnnotatedString {
                        append(title)
                        if (lit && picked != null) {
                            withStyle(SpanStyle(color = CapsuleInk, fontWeight = FontWeight.Normal)) {
                                append(" — $picked")
                            }
                        }
                    },
                    color = if (lit) Color.White else Color(0xFF8A8A90),
                    fontWeight = if (lit) FontWeight.Bold else FontWeight.Normal,
                    fontSize = CategoryTabFontSize,
                    lineHeight = CategoryTabLineHeight,
                    letterSpacing = CategoryTabLetterSpacing,
                )
                if (tab.hasGenres && lit) {
                    MenuArrow(
                        open = filtersOpen && lit,
                        tint = if (lit && picked == null) Color.White else CapsuleInk,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryFilterDialog(
    open: Boolean,
    title: String,
    genres: List<String>,
    genre: String?,
    sort: LibrarySort?,
    onGenre: (String?) -> Unit,
    onSort: (LibrarySort) -> Unit,
    onDismiss: () -> Unit,
) {
    val visible = remember { MutableTransitionState(false) }
    LaunchedEffect(open) { visible.targetState = open }
    if (!visible.currentState && !visible.targetState) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.apply {
                setWindowAnimations(0)
                setDimAmount(0f)
                setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            }
        }
        AnimatedVisibility(
            visibleState = visible,
            enter = PeekEnter,
            exit = PeekExit,
            modifier = Modifier.fillMaxSize(),
        ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
            Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 22.dp)
                        .fillMaxWidth()
                        .clip(FilterShape)
                        .background(CinemaSurface)
                        .border(0.5.dp, Color.White.copy(alpha = 0.10f), FilterShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 26.sp,
                        letterSpacing = (-0.4).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.favorites_sort),
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        color = CinemaMuted,
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LibrarySort.entries.forEach { option ->
                            FilterChip(
                                label = stringResource(option.label),
                                chosen = option == sort,
                                onClick = { onSort(option) },
                            )
                        }
                    }
                    if (genres.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.10f)),
                        )
                        Text(
                            text = stringResource(R.string.favorites_genres),
                            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            color = CinemaMuted,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                genres.forEach { label ->
                                    FilterChip(
                                        label = label,
                                        chosen = label.lowercase() == genre,
                                        onClick = {
                                            onGenre(if (label.lowercase() == genre) null else label.lowercase())
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

@Composable
private fun FilterChip(
    label: String,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (chosen) Color.White else FilterNotes,
        fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (chosen) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember(label) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun MenuArrow(
    open: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val rotation by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(220),
        label = "menuArrow",
    )
    Icon(
        imageVector = Icons.Filled.KeyboardArrowDown,
        contentDescription = stringResource(R.string.favorites_genres),
        tint = tint,
        modifier = modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation },
    )
}

private fun LazyListState.passedTab(): Int {
    val head = layoutInfo.visibleItemsInfo.firstOrNull() ?: return firstVisibleItemIndex
    return if (head.offset + head.size / 2 < 0) head.index + 1 else head.index
}

@Composable
private fun FavoritePoster(
    item: SavedTitle,
    badge: String? = null,
    progress: Float? = null,
    caption: String? = null,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    val tick = rememberHaptic(HapticSlot.Poster)
    var holdProgress by remember(item.id) { mutableFloatStateOf(0f) }
    var pressed by remember(item.id) { mutableStateOf(false) }
    val pressScale = rememberPressScale(pressed)
    val wave = remember(item.id) { PosterWave() }
    val showTitles = posterTitlesOn()
    Column {
        Box {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / 1.46f)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                    .clip(PosterShape)
                    .background(Color.White.copy(alpha = 0.06f)),
            ) {
                PosterImage(
                    url = item.posterUrl,
                    movieId = item.id,
                    contentDescription = stringResource(R.string.poster_content_description, item.title),
                    modifier = Modifier
                        .matchParentSize()
                        .posterWave(wave)
                        .holdToPeek(
                            onPeek = { onMoviePeek(item.toMovie()) },
                            onProgress = { holdProgress = it },
                            onPressed = { pressed = it },
                            onClick = {
                                tick?.invoke()
                                DetailSeed.put(item.toMovie())
                                onMovieClick(item.id)
                            },
                            wave = wave,
                        )
                        .holdCue(holdProgress),
                )
                if (progress != null || !caption.isNullOrBlank()) {
                    WatchProgress(
                        fraction = progress ?: 0f,
                        caption = caption,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            if (badge != null) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(WatchedBadgeHang))
        if (showTitles) PosterCaption(item.title, item.year)
    }
}
