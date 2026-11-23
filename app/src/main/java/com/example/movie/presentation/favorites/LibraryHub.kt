package com.example.movie.presentation.favorites

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.domain.model.LibraryShelves
import com.example.movie.domain.model.normalizedLibraryOrder
import com.example.movie.presentation.common.BookmarkFilled
import com.example.movie.presentation.common.PlexPlay
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.common.appChrome
import com.example.movie.data.download.OfflineItem
import com.example.movie.data.download.OfflinePhase
import com.example.movie.data.download.OfflineStatus
import com.example.movie.presentation.common.glideDownloadPercent
import com.example.movie.presentation.common.inProgress
import com.example.movie.presentation.common.drawTopScrim
import com.example.movie.presentation.common.scrollToTopOn
import com.example.movie.presentation.common.headerThemeBrush
import com.example.movie.presentation.common.headerFillOn
import com.example.movie.presentation.common.topScrimFade
import com.example.movie.presentation.common.topScrimHeight
import com.example.movie.presentation.common.topScrimPalette
import com.example.movie.ui.theme.PlayBlue
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.ResumeItem
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.progressCaption
import com.example.movie.domain.model.toMovie
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.SectionScrim
import com.example.movie.presentation.home.components.RecommendationRail
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.LogoAspect
import com.example.movie.presentation.home.components.LogoHeight
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.HomeContentInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.BrandWordmark
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface

@Composable
internal fun LibraryHub(
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
    folders: List<IndexedFolder>,
    onOpenFolders: () -> Unit,
    onOpenFolder: (Int) -> Unit,
    onEditFolder: (Int) -> Unit,
    onDeleteFolder: (Int) -> Unit,
    onAddFolder: () -> Unit,
    scrollTop: Int,
) {
    val scroll = rememberScrollState()
    scroll.scrollToTopOn(scrollTop)
    val header = libraryTopPadding()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val order = settings.libraryOrder.normalizedLibraryOrder()
    var orderSheet by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = header, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            order.forEach { id ->
                when (id) {
                    LibraryShelves.Favorites -> if (favorites.isEmpty()) {
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
                    LibraryShelves.Continue -> if (resume.isEmpty()) {
                        LibraryPlaque(
                            title = stringResource(R.string.section_continue),
                            body = stringResource(R.string.library_continue_empty),
                            icon = PlexPlay,
                            iconSize = 92.dp,
                            onClick = onOpenCatalog,
                        )
                    } else {
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
                    LibraryShelves.History -> if (history.isEmpty()) {
                        LibraryPlaque(
                            title = stringResource(R.string.section_history),
                            body = stringResource(R.string.library_history_empty),
                            icon = ShellIcons.history,
                            iconSize = 92.dp,
                            onClick = onOpenCatalog,
                        )
                    } else {
                        LibraryRail(
                            title = stringResource(R.string.section_history),
                            movies = history.take(LibraryRailCap).map { it.toMovie() },
                            onMovieClick = onMovieClick,
                            onMoviePeek = onMoviePeek,
                            onOpen = { onOpen(LibrarySection.History) },
                        )
                    }
                    LibraryShelves.Watched -> if (watched.isEmpty()) {
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
                    LibraryShelves.Downloads -> if (downloads.isEmpty()) {
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
                            onMovieClick = { movieId ->
                                downloads.firstOrNull { it.title.id == movieId }?.let(onOpenDownload)
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
                    LibraryShelves.Folders -> if (folders.isEmpty()) {
                        LibraryPlaque(
                            title = stringResource(R.string.library_folders),
                            icon = FolderMark,
                            iconSize = 92.dp,
                            onCreate = onAddFolder,
                        )
                    } else {
                        FolderRail(
                            folders = folders,
                            onOpen = onOpenFolders,
                            onOpenFolder = onOpenFolder,
                            onEditFolder = onEditFolder,
                            onDeleteFolder = onDeleteFolder,
                            onAdd = onAddFolder,
                        )
                    }
                }
            }
        }
        SectionScrim(
            progress = scroll.libraryScrim(),
            modifier = Modifier.align(Alignment.TopCenter),
        )
        LibraryBar(
            title = stringResource(R.string.nav_favorites),
            onBack = onBack,
            onOrder = { orderSheet = true },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        if (orderSheet) {
            LibraryOrderSheet(onDismiss = { orderSheet = false })
        }
    }
}

@Composable
internal fun LibraryBar(
    title: String,
    onBack: () -> Unit,
    onOrder: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    mark: ImageVector? = null,
    titleAtStart: Boolean = false,
    titleColor: Color = Color.White,
    markTint: Color = Color.White,
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
                .align(if (titleAtStart) Alignment.CenterStart else Alignment.Center)
                .padding(
                    start = if (titleAtStart) PosterGridInset + 29.dp else 56.dp,
                    end = if (titleAtStart) 156.dp else 56.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mark != null) {
                Icon(
                    imageVector = mark,
                    contentDescription = null,
                    tint = markTint,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = title,
                color = titleColor,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        if (onOrder != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = HomeContentInset - 9.dp)
                    .offset(y = 2.dp)
                    .requiredSize(40.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onOrder()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ShellIcons.settings,
                    contentDescription = stringResource(R.string.library_order_open),
                    tint = titleColor,
                    modifier = Modifier.size(28.dp),
                )
            }
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
internal fun LibraryRail(
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
internal fun DownloadRailMark(status: OfflineStatus) {
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
internal fun DownloadPercent(percent: Int) {
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
internal fun LibraryPlaque(
    title: String,
    body: String? = null,
    icon: ImageVector? = null,
    iconSize: Dp = 72.dp,
    action: String? = null,
    onClick: (() -> Unit)? = null,
    onAdd: (() -> Unit)? = null,
    onCreate: (() -> Unit)? = null,
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
            if (onCreate != null) {
                FolderTitlePlus(
                    onClick = onCreate,
                    iconSize = 32.dp,
                    textSize = 22.sp,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
        }
    }
}

@Composable
internal fun LibraryBrand(
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
internal fun LibraryScrim(progress: Float, modifier: Modifier = Modifier) {
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

@Composable
internal fun LibraryMenu(
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
internal fun LibraryPickerSheet(
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

