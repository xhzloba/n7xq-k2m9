package com.example.movie.presentation.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.data.folders.LibraryFolder
import com.example.movie.domain.model.SavedTitle
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.presentation.common.LocalShelfNotices
import com.example.movie.presentation.common.NoticeShelfFavorites
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PlayBlue

@Composable
internal fun FolderPickSheet(
    title: SavedTitle,
    onDismiss: () -> Unit,
) {
    val store = LocalAppContainer.current.folders
    val folders by store.items.collectAsStateWithLifecycle()
    val tick = rememberHapticTick()
    val notices = LocalShelfNotices.current
    val context = LocalContext.current
    var creating by remember { mutableStateOf(folders.isEmpty()) }
    var name by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    val ready = name.isNotBlank()
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
                .padding(bottom = 6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.menu_list),
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                creating = !creating
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.library_folder_new),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            val placed = folders.mapIndexedNotNull { index, folder ->
                if (folder.titles.any { it.id == title.id }) index to folder else null
            }
            val rest = folders.mapIndexedNotNull { index, folder ->
                if (folder.titles.none { it.id == title.id }) index to folder else null
            }
            val pick: (Int, LibraryFolder) -> Unit = { index, folder ->
                tick?.invoke()
                val now = store.toggle(index, title)
                notices.show(
                    context.getString(
                        if (now) R.string.notice_folder_add else R.string.notice_folder_remove,
                        folder.name,
                    ),
                    NoticeShelfFavorites,
                )
            }
            Column(
                Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (creating) {
                    FolderLine(
                        value = name,
                        onValueChange = { name = it.replace('\n', ' ') },
                        label = stringResource(R.string.library_folder_name),
                        singleLine = true,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                    )
                    FolderLine(
                        value = about,
                        onValueChange = { about = it },
                        label = stringResource(R.string.library_folder_about),
                        singleLine = false,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
                    )
                    Text(
                        text = stringResource(R.string.library_folder_create),
                        color = Color.White.copy(alpha = if (ready) 1f else 0.28f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 12.dp, top = 4.dp, bottom = 6.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = ready,
                                onClick = {
                                    tick?.invoke()
                                    val named = name.trim()
                                    store.add(named, about.trim(), title)
                                    notices.show(
                                        context.getString(R.string.notice_folder_add, named),
                                        NoticeShelfFavorites,
                                    )
                                    name = ""
                                    about = ""
                                    creating = false
                                },
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                if (placed.isNotEmpty()) {
                    FolderRail(
                        label = stringResource(R.string.folder_added_in),
                        movieId = title.id,
                        folders = placed,
                        onPick = pick,
                    )
                }
                if (rest.isNotEmpty()) {
                    FolderRail(
                        label = if (placed.isEmpty()) null else stringResource(R.string.folder_not_added),
                        movieId = title.id,
                        folders = rest,
                        onPick = pick,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FolderTrail(count: Int, name: String?, tint: Color) {
    if (count <= 0) return
    if (count > 1) {
        Text(
            text = count.toString(),
            color = tint,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(6.dp))
    }
    if (!name.isNullOrBlank()) {
        Text(
            text = name,
            color = tint,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 96.dp),
        )
        Spacer(Modifier.width(2.dp))
    }
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun FolderRail(
    label: String?,
    movieId: String,
    folders: List<Pair<Int, LibraryFolder>>,
    onPick: (Int, LibraryFolder) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                color = CinemaMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            folders.forEach { (index, folder) ->
                FolderSlide(
                    folder = folder,
                    inside = folder.titles.any { it.id == movieId },
                    onClick = { onPick(index, folder) },
                )
            }
        }
    }
}

@Composable
private fun FolderSlide(
    folder: LibraryFolder,
    inside: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = FolderGlyph,
            contentDescription = null,
            tint = if (inside) PlayBlue else CinemaMuted,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = folder.name,
            color = if (inside) Color.White else CinemaMuted,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 240.dp),
        )
        if (inside) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = PlayBlue,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp),
            )
        }
    }
}

@Composable
internal fun FolderCreateSheet(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
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
                    text = stringResource(R.string.library_folder_new),
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            FolderLine(
                value = name,
                onValueChange = { name = it.replace('\n', ' ') },
                label = stringResource(R.string.library_folder_name),
                singleLine = true,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            )
            FolderLine(
                value = about,
                onValueChange = { about = it },
                label = stringResource(R.string.library_folder_about),
                singleLine = false,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
            )
            Text(
                text = stringResource(R.string.library_folder_create),
                color = Color.White.copy(alpha = if (ready) 1f else 0.28f),
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp, top = 4.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = ready,
                        onClick = {
                            tick?.invoke()
                            onCreate(name.trim(), about.trim())
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun FolderLine(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    val focus = remember { MutableInteractionSource() }
    val focused by focus.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        interactionSource = focus,
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        ),
        cursorBrush = SolidColor(Color.White),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (singleLine) 36.dp else 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                if (value.isEmpty() && !focused) {
                    Text(
                        text = label,
                        color = CinemaMuted,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                    )
                }
                inner()
            }
        },
    )
}

internal val FolderGlyph: ImageVector = ImageVector.Builder(
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
