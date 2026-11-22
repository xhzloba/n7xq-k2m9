package com.example.movie.presentation.favorites

import java.text.Collator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.data.folders.LibraryFolder
import com.example.movie.presentation.folders.FolderGlyph
import com.example.movie.presentation.folders.FolderLookPicker
import com.example.movie.presentation.common.scrollToTopOn
import com.example.movie.ui.theme.PlayBlue
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.SavedTitle
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.presentation.home.SectionScrim
import com.example.movie.presentation.home.components.RailSeeAllCard
import com.example.movie.presentation.home.components.RailTitle
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.PosterGap
import com.example.movie.presentation.home.components.posterGridColumns
import com.example.movie.ui.theme.CinemaMuted
import kotlin.math.roundToInt

internal enum class FolderOrder { Date, Name }

internal enum class FolderTitlesOrder { Added, Year, Title, Rating, Unwatched }

internal fun List<SavedTitle>.ordered(
    mode: FolderTitlesOrder,
    watched: Set<String> = emptySet(),
): List<SavedTitle> = when (mode) {
    FolderTitlesOrder.Added -> this
    FolderTitlesOrder.Unwatched -> sortedBy { it.id in watched }
    FolderTitlesOrder.Year -> sortedWith(
        compareByDescending<SavedTitle> { it.year != null }
            .thenByDescending { it.year ?: 0 }
            .thenBy { it.title.lowercase() },
    )
    FolderTitlesOrder.Title -> sortedWith(
        compareBy(Collator.getInstance(Locale.forLanguageTag("ru"))) { it.title },
    )
    FolderTitlesOrder.Rating -> sortedWith(
        compareByDescending<SavedTitle> { it.ratingKp != null || it.ratingImdb != null }
            .thenByDescending { maxOf(it.ratingKp ?: 0f, it.ratingImdb ?: 0f) }
            .thenBy { it.title.lowercase() },
    )
}

internal data class IndexedFolder(val index: Int, val folder: LibraryFolder)

internal fun List<LibraryFolder>.ordered(mode: FolderOrder): List<IndexedFolder> {
    val rows = mapIndexed { index, folder -> IndexedFolder(index, folder) }
    return when (mode) {
        FolderOrder.Date -> rows.sortedWith(
            compareByDescending<IndexedFolder> { it.folder.touchedAt }.thenBy { it.index },
        )
        FolderOrder.Name -> rows.sortedWith(
            compareBy(Collator.getInstance(Locale.forLanguageTag("ru"))) { it.folder.name },
        )
    }
}

internal val FolderSortIcon: ImageVector = ImageVector.Builder(
    name = "folderSort",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).path(fill = SolidColor(Color.Black)) {
    moveTo(3f, 6f)
    horizontalLineTo(21f)
    verticalLineTo(8f)
    horizontalLineTo(3f)
    close()
    moveTo(6f, 11f)
    horizontalLineTo(18f)
    verticalLineTo(13f)
    horizontalLineTo(6f)
    close()
    moveTo(9f, 16f)
    horizontalLineTo(15f)
    verticalLineTo(18f)
    horizontalLineTo(9f)
    close()
}.build()

internal val FolderDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru"))

internal fun Long.folderDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(FolderDateFormat).replace(".", "")

@Composable
internal fun FolderSortButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberHapticTick()
    Box(
        modifier = modifier
            .size(32.dp)
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
            imageVector = FolderSortIcon,
            contentDescription = stringResource(R.string.library_folder_sort),
            tint = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun FolderSortSheet(
    current: FolderOrder,
    onPick: (FolderOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val tick = rememberHapticTick()
    HandleSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.library_folder_sort),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        FolderOrder.entries.forEach { mode ->
            val label = stringResource(
                if (mode == FolderOrder.Name) R.string.library_folder_sort_name else R.string.library_folder_sort_date,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onPick(mode)
                        },
                    )
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                )
                if (mode == current) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = PlayBlue,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun FolderTitlesSortSheet(
    current: FolderTitlesOrder,
    onPick: (FolderTitlesOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    val tick = rememberHapticTick()
    HandleSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.library_folder_sort),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        FolderTitlesOrder.entries.forEach { mode ->
            val label = stringResource(
                when (mode) {
                    FolderTitlesOrder.Added -> R.string.library_folder_sort_added
                    FolderTitlesOrder.Unwatched -> R.string.library_folder_sort_unwatched
                    FolderTitlesOrder.Year -> R.string.favorites_sort_year
                    FolderTitlesOrder.Title -> R.string.favorites_sort_title
                    FolderTitlesOrder.Rating -> R.string.catalog_sort_rating
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onPick(mode)
                        },
                    )
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                )
                if (mode == current) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = PlayBlue,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun FolderAdd(
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
internal fun FolderTitlePlus(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 22.dp,
    textSize: TextUnit = 15.sp,
    compact: Boolean = false,
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
            .padding(horizontal = 4.dp, vertical = if (compact) 0.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.library_folder_new),
            color = Color.White,
            fontSize = textSize,
            lineHeight = textSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
internal fun FolderRail(
    folders: List<IndexedFolder>,
    onOpen: () -> Unit,
    onOpenFolder: (Int) -> Unit,
    onEditFolder: (Int) -> Unit,
    onDeleteFolder: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val width = 228.dp
    val shown = folders.take(LibraryRailCap)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val height = shown.maxOf { row -> measureFolderPlateHeight(measurer, density, row.folder, width) }
    val listState = rememberLazyListState()
    var known by remember { mutableIntStateOf(folders.size) }
    LaunchedEffect(folders.size) {
        if (folders.size > known) listState.animateScrollToItem(0)
        known = folders.size
    }
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            RailTitle(
                title = stringResource(R.string.library_folders),
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth(),
            )
            FolderTitlePlus(
                onClick = onAdd,
                iconSize = 16.dp,
                textSize = 13.sp,
                compact = true,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = PosterGridInset),
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(start = PosterGridInset, end = PosterGridInset),
            horizontalArrangement = Arrangement.spacedBy(PosterGap),
        ) {
            items(shown, key = { row -> row.folder.id.ifBlank { "${row.index}:${row.folder.name}" } }) { row ->
                FolderCard(
                    folder = row.folder,
                    plateHeight = height,
                    modifier = Modifier.width(width),
                    onClick = { onOpenFolder(row.index) },
                    onEdit = { onEditFolder(row.index) },
                    onDelete = { onDeleteFolder(row.index) },
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
internal fun FolderCard(
    folder: LibraryFolder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    plateHeight: Dp? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val tick = rememberHapticTick()
    val tinted = folder.color != 0
    val covers = if (folder.covers) {
        folder.titles.mapNotNull { it.posterUrl?.takeIf { url -> url.isNotBlank() } }.distinct().take(3)
    } else {
        emptyList()
    }
    Column(
        modifier.clickable(
            interactionSource = remember(folder.name) { MutableInteractionSource() },
            indication = null,
            onClick = {
                tick?.invoke()
                onClick()
            },
        ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .then(if (plateHeight != null) Modifier.height(plateHeight) else Modifier)
                .clip(RoundedCornerShape(16.dp))
                .background(folder.folderWash()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 14.dp,
                        end = if (covers.isNotEmpty()) 108.dp else 14.dp,
                        top = 12.dp,
                        bottom = if (covers.isNotEmpty()) 90.dp else 12.dp,
                    ),
            ) {
                Icon(
                    imageVector = FolderMark,
                    contentDescription = null,
                    tint = if (tinted) Color(folder.color.folderInk()) else Color.White.copy(alpha = 0.28f),
                    modifier = Modifier.size(26.dp),
                )
                if (folder.description.isNotBlank()) {
                    Text(
                        text = folder.description,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        color = if (tinted) Color.White.copy(alpha = 0.72f) else CinemaMuted,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (covers.isNotEmpty()) {
                FolderCovers(
                    urls = covers,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 10.dp),
                )
            }
            if (onEdit != null || onDelete != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onEdit != null) {
                        FolderCardAction(
                            icon = Icons.Filled.Edit,
                            description = stringResource(R.string.library_folder_edit),
                            onClick = {
                                tick?.invoke()
                                onEdit()
                            },
                        )
                    }
                    if (onDelete != null) {
                        FolderCardAction(
                            icon = Icons.Filled.Delete,
                            description = stringResource(R.string.library_folder_delete),
                            onClick = {
                                tick?.invoke()
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
        Text(
            text = folder.name,
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (folder.createdAt > 0L) {
            Text(
                text = folder.createdAt.folderDate(),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .fillMaxWidth(),
                color = CinemaMuted,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 1,
            )
        }
    }
}

internal val FolderAboutStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 17.sp,
)

internal fun measureFolderPlateHeight(
    measurer: TextMeasurer,
    density: Density,
    folder: LibraryFolder,
    width: Dp,
): Dp {
    val textWidth = with(density) { (width - 28.dp).roundToPx().coerceAtLeast(0) }
    val about = if (folder.description.isBlank()) {
        null
    } else {
        measurer.measure(
            text = folder.description,
            style = FolderAboutStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            constraints = Constraints(maxWidth = textWidth),
        )
    }
    val hasCovers = folder.covers && folder.titles.any { !it.posterUrl.isNullOrBlank() }
    return with(density) {
        12.dp + 26.dp +
            (about?.let { 8.dp + it.size.height.toDp() } ?: 0.dp) +
            if (hasCovers) 94.dp else 12.dp
    }
}

@Composable
internal fun FolderCovers(urls: List<String>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val order = when (urls.size) {
        3 -> listOf(0, 2, 1)
        else -> urls.indices.toList()
    }
    Box(modifier.size(width = 108.dp, height = 82.dp)) {
        order.forEach { index ->
            val front = urls.size == 1 || index == urls.lastIndex && urls.size == 2 || index == 1
            val lean = when (urls.size) {
                1 -> 0f
                2 -> if (index == 0) -6f else 5f
                else -> when (index) {
                    0 -> -8f
                    2 -> 8f
                    else -> 0f
                }
            }
            AsyncImage(
                model = urls[index],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (index * 16).dp, y = if (front) 0.dp else 4.dp)
                    .graphicsLayer {
                        rotationZ = lean
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        scaleX = if (front) 1f else 0.94f
                        scaleY = if (front) 1f else 0.94f
                        alpha = if (front) 1f else 0.86f
                        shadowElevation = if (front) 18f else 8f
                        this.shape = shape
                        clip = false
                    }
                    .size(width = 52.dp, height = 74.dp)
                    .clip(shape),
            )
        }
    }
}

@Composable
internal fun FolderPage(
    folders: List<IndexedFolder>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onSort: () -> Unit,
    scrollTop: Int,
) {
    val scroll = rememberScrollState()
    scroll.scrollToTopOn(scrollTop)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val cardWidth = (maxWidth - PosterGridInset * 2 - 10.dp) / 2
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = libraryTopPadding(), bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            folders.chunked(2).forEach { row ->
                val height = row.maxOf { item ->
                    measureFolderPlateHeight(measurer, density, item.folder, cardWidth)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = PosterGridInset),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    row.forEach { item ->
                        FolderCard(
                            folder = item.folder,
                            plateHeight = height,
                            onClick = { onOpen(item.index) },
                            onEdit = { onEdit(item.index) },
                            onDelete = { onDelete(item.index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
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
            titleAtStart = true,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                .padding(top = BrandTopInset, end = PosterGridInset)
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolderSortButton(onClick = onSort)
            FolderTitlePlus(onClick = onAdd)
        }
    }
}

@Composable
internal fun FolderInside(
    folder: LibraryFolder,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
) {
    var sortOpen by remember { mutableStateOf(false) }
    var orderName by rememberSaveable(folder.id) { mutableStateOf(FolderTitlesOrder.Added.name) }
    val order = runCatching { FolderTitlesOrder.valueOf(orderName) }.getOrDefault(FolderTitlesOrder.Added)
    val watched by LocalAppContainer.current.watched.ids.collectAsStateWithLifecycle()
    val titles = folder.titles.ordered(order, watched)
    val watchedCount = folder.titles.count { it.id in watched }
    val ink = if (folder.color != 0) Color(folder.color.folderInk()) else Color.White
    val pace = if (folder.color != 0) ink else PlayBlue
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
                item(key = "pace", span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(bottom = 8.dp)) {
                        FolderPace(
                            done = watchedCount,
                            total = folder.titles.size,
                            ink = pace,
                        )
                        if (folder.description.isNotBlank()) {
                            Text(
                                text = folder.description,
                                color = CinemaMuted,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                    }
                }
                items(titles, key = { it.id }) { item ->
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
            titleAtStart = true,
            titleColor = ink,
            markTint = ink,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        if (folder.titles.isNotEmpty()) {
            FolderSortButton(
                onClick = { sortOpen = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
                    .padding(top = BrandTopInset, end = PosterGridInset),
            )
        }
        if (sortOpen) {
            FolderTitlesSortSheet(
                current = order,
                onPick = {
                    orderName = it.name
                    sortOpen = false
                },
                onDismiss = { sortOpen = false },
            )
        }
    }
}

@Composable
private fun FolderPace(done: Int, total: Int, ink: Color) {
    val target = if (total == 0) 0f else done.toFloat() / total.toFloat()
    val fraction by animateFloatAsState(target, tween(420), label = "folderPace")
    val percent = (target * 100f).roundToInt().coerceIn(0, 100)
    val shape = RoundedCornerShape(percent = 50)
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_folder_pace),
                modifier = Modifier.weight(1f),
                color = CinemaMuted,
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
            Text(
                text = stringResource(R.string.library_folder_pace_count, percent),
                color = Color.White.copy(alpha = 0.42f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
        }
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(shape)
                .background(Color.White.copy(alpha = 0.10f)),
        ) {
            if (fraction > 0.001f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                        .height(6.dp)
                        .clip(shape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(ink.copy(alpha = 0.55f), ink),
                            ),
                        ),
                )
            }
        }
        Text(
            text = stringResource(R.string.library_folder_pace_of, done, total),
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 6.dp),
            color = Color.White.copy(alpha = 0.42f),
            fontSize = 12.sp,
            lineHeight = 15.sp,
        )
    }
}

@Composable
internal fun FolderCardAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White.copy(alpha = 0.46f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
internal fun FolderDeleteSheet(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val tick = rememberHapticTick()
    HandleSheet(onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.library_folder_delete_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.library_folder_delete_body, name),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 8.dp),
            color = CinemaMuted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
        )
        FolderDeleteRow(
            label = stringResource(R.string.library_folder_delete_confirm),
            onClick = {
                tick?.invoke()
                onConfirm()
            },
        )
        FolderDeleteRow(
            label = stringResource(R.string.library_folder_cancel),
            onClick = {
                tick?.invoke()
                onDismiss()
            },
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun FolderDeleteRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            maxLines = 1,
        )
    }
}

internal fun Int.folderInk(): Int = if (this == 0xFF0A84FF.toInt()) 0xFF3B6EF5.toInt() else this

internal fun LibraryFolder.folderWash(): Brush {
    if (color == 0) {
        val glass = Color.White.copy(alpha = 0.06f)
        return Brush.verticalGradient(listOf(glass, glass))
    }
    val ink = Color(color.folderInk())
    return Brush.verticalGradient(
        0f to ink.copy(alpha = 0.62f),
        1f to ink.copy(alpha = 0.40f),
    )
}

@Composable
internal fun FolderNameDialog(
    initial: LibraryFolder?,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Boolean) -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var about by remember(initial) { mutableStateOf(initial?.description.orEmpty()) }
    var color by remember(initial) { mutableIntStateOf(initial?.color?.folderInk() ?: 0) }
    var coversOn by remember(initial) { mutableStateOf(initial?.covers != false) }
    val ready = name.isNotBlank()
    val tick = rememberHapticTick()
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .padding(bottom = 6.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FolderGlyph,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.library_folder_edit),
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            FolderField(
                value = name,
                onValueChange = { name = it.replace('\n', ' ') },
                label = stringResource(R.string.library_folder_name),
                singleLine = true,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            )
            FolderField(
                value = about,
                onValueChange = { about = it },
                label = stringResource(R.string.library_folder_about),
                singleLine = false,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
            )
            FolderLookPicker(
                coversOn = coversOn,
                onCovers = { coversOn = it },
                color = color,
                onColor = { color = it },
                modifier = Modifier.padding(top = 14.dp),
                resetKey = initial?.id,
            )
            Text(
                text = stringResource(R.string.library_folder_save),
                color = Color.White.copy(alpha = if (ready) 1f else 0.28f),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp, top = 6.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = ready,
                        onClick = { onSave(name.trim(), about.trim(), color, coversOn) },
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
internal fun FolderField(
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

internal val FolderMark: ImageVector = ImageVector.Builder(
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

