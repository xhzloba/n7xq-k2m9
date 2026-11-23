package com.example.movie.presentation.favorites

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.data.download.OfflineItem
import com.example.movie.presentation.folders.FolderCreateSheet
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.common.scrollToTopOn
import com.example.movie.presentation.detail.OfflinePlayer
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.progressCaption
import com.example.movie.domain.model.toMovie
import com.example.movie.domain.model.toSaved
import com.example.movie.presentation.home.SectionScrim
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.PosterGap
import com.example.movie.presentation.home.components.posterGridColumns
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaMuted

private enum class LibrarySheet { Sort, Filter }

private fun LazyGridState.keepPlace() {
    // Иначе грид держит первый видимый постер по ключу и уезжает за ним вниз.
    requestScrollToItem(firstVisibleItemIndex, firstVisibleItemScrollOffset)
}

@Composable
internal fun FavoritesScreen(
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onPlayResume: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    openFolderId: String? = null,
    onFolderOpened: () -> Unit = {},
    handleBack: Boolean = true,
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
    var folderOrderName by rememberSaveable { mutableStateOf(FolderOrder.Date.name) }
    var folderSortOpen by remember { mutableStateOf(false) }
    val folderOrder = FolderOrder.valueOf(folderOrderName)
    val orderedFolders = folders.ordered(folderOrder)
    var foldersOpen by rememberSaveable { mutableStateOf(false) }
    var openedFolder by rememberSaveable { mutableIntStateOf(-1) }
    var editingFolder by remember { mutableStateOf<Int?>(null) }
    var deletingFolder by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(openFolderId, folders) {
        val id = openFolderId ?: return@LaunchedEffect
        val index = folders.indexOfFirst { it.id == id }
        if (index < 0) return@LaunchedEffect
        libraryName = ""
        foldersOpen = false
        openedFolder = index
        onFolderOpened()
    }
    BackHandler(enabled = handleBack && (library != null || foldersOpen || openedFolder >= 0)) {
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
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    var kindRaw by rememberSaveable { mutableStateOf("") }
    var sortRaw by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf<LibrarySheet?>(null) }
    val sectionKey = library?.name
    val kinds = items.presentKinds()
    val kind = sectionKey?.let { decodeFilters(kindRaw)[it] }
        ?.let { runCatching { LibraryKind.valueOf(it) }.getOrNull() }
        ?.takeIf { it in kinds }
    val sort = sectionKey?.let { decodeFilters(sortRaw)[it] }
        ?.let { runCatching { LibrarySort.valueOf(it) }.getOrNull() }
        ?.takeIf { it != LibrarySort.Release || settings.showReleaseDate }
    val shown = items
        .let { list -> kind?.let { picked -> list.filter { it.libraryKind() == picked } } ?: list }
        .sortedFor(sort)
    val sorts = buildList {
        add(LibrarySort.Year)
        add(LibrarySort.Title)
        add(LibrarySort.Rating)
        if (settings.showReleaseDate) add(LibrarySort.Release)
    }
    val gridState = rememberLazyGridState()
    gridState.scrollToTopOn(scrollTop)
    val closePage = {
        openedDownload = null
        libraryName = ""
        sheet = null
    }
    val listTools = library != null && library != LibrarySection.Downloads && items.isNotEmpty()
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
                folders = orderedFolders,
                onBack = { foldersOpen = false },
                onAdd = { editingFolder = -1 },
                onOpen = { openedFolder = it },
                onEdit = { editingFolder = it },
                onDelete = { deletingFolder = it },
                onSort = { folderSortOpen = true },
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
                folders = orderedFolders,
                downloadStatus = downloadStatus,
                onOpenFolders = { foldersOpen = true },
                onOpenFolder = { openedFolder = it },
                onEditFolder = { editingFolder = it },
                onDeleteFolder = { deletingFolder = it },
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
                onSort = if (listTools) ({ sheet = LibrarySheet.Sort }) else null,
                onFilter = if (listTools && kinds.isNotEmpty()) ({ sheet = LibrarySheet.Filter }) else null,
                sortOn = sort != null,
                filterOn = kind != null,
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
                onSort = if (listTools) ({ sheet = LibrarySheet.Sort }) else null,
                onFilter = if (listTools && kinds.isNotEmpty()) ({ sheet = LibrarySheet.Filter }) else null,
                sortOn = sort != null,
                filterOn = kind != null,
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
        if (sheet == LibrarySheet.Sort && sectionKey != null) {
            LibraryOptionSheet(
                title = stringResource(R.string.favorites_sort),
                options = listOf("" to stringResource(R.string.catalog_sort_default)) +
                    sorts.map { it.name to stringResource(it.label) },
                selected = sort?.name.orEmpty(),
                onPick = { id ->
                    val next = id.takeIf { it.isNotEmpty() && it != sort?.name }
                    if (next != sort?.name) gridState.keepPlace()
                    sortRaw = encodeFilters(decodeFilters(sortRaw).with(sectionKey, next))
                    sheet = null
                },
                onDismiss = { sheet = null },
            )
        }
        if (sheet == LibrarySheet.Filter && sectionKey != null) {
            LibraryOptionSheet(
                title = stringResource(R.string.favorites_filter),
                options = listOf("" to stringResource(R.string.catalog_sort_default)) +
                    kinds.map { it.name to stringResource(it.label) },
                selected = kind?.name.orEmpty(),
                onPick = { id ->
                    val next = id.takeIf { it.isNotEmpty() && it != kind?.name }
                    if (next != kind?.name) gridState.keepPlace()
                    kindRaw = encodeFilters(decodeFilters(kindRaw).with(sectionKey, next))
                    sheet = null
                },
                onDismiss = { sheet = null },
            )
        }
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
                onSave = { name, about, color, covers ->
                    container.folders.replace(
                        folders.toMutableList().also {
                            it[editor] = folders[editor].copy(
                                name = name,
                                description = about,
                                color = color,
                                covers = covers,
                            )
                        },
                    )
                    editingFolder = null
                },
            )
        } else if (editor != null) {
            FolderCreateSheet(
                onDismiss = { editingFolder = null },
                onCreate = { name, about, color, covers ->
                    container.folders.add(name, about, null, color, covers)
                    editingFolder = null
                },
            )
        }
        val removing = deletingFolder
        if (removing != null && removing in folders.indices) {
            val folder = folders[removing]
            FolderDeleteSheet(
                name = folder.name,
                onConfirm = {
                    container.folders.remove(folder.id)
                    when {
                        openedFolder == removing -> openedFolder = -1
                        openedFolder > removing -> openedFolder -= 1
                    }
                    deletingFolder = null
                },
                onDismiss = { deletingFolder = null },
            )
        }
        if (folderSortOpen) {
            FolderSortSheet(
                current = folderOrder,
                onPick = {
                    folderOrderName = it.name
                    folderSortOpen = false
                },
                onDismiss = { folderSortOpen = false },
            )
        }
    }
}
