package com.example.movie.presentation.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.data.backdrops.BackdropPick
import com.example.movie.data.backdrops.GoodBackdropWidth
import com.example.movie.data.backdrops.toBackdropKey
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
internal fun BackdropFramesPage(
    movieId: String,
    api: List<String>,
    poster: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = LocalAppContainer.current.backdropSets
    val picks by store.sets.collectAsStateWithLifecycle()
    val pick = picks[movieId] ?: BackdropPick()
    val tick = rememberHapticTick()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val top = with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val grid = rememberLazyGridState()
    val headerScrim = if (grid.firstVisibleItemIndex > 0) 1f
    else (grid.firstVisibleItemScrollOffset / with(density) { 28.dp.toPx() }).coerceIn(0f, 1f)
    val slots = remember(api, pick.own) {
        api.map { FrameSlot(it, it, null) } + pick.own.filter { File(it).isFile }.map { path ->
            val key = path.toBackdropKey()
            FrameSlot(key, File(path), path)
        }
    }
    val add = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            store.addOwn(movieId, uri, context.contentResolver)
        }
    }
    val addPoster = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            store.addPoster(movieId, uri, context.contentResolver)
        }
    }
    val posters = remember(poster, pick.posterOwn) {
        buildList {
            val catalog = poster?.takeIf { it.isNotBlank() }
            if (catalog != null) add(PosterSlot(catalog, catalog, null))
            pick.posterOwn.filter { File(it).isFile }.forEach { path ->
                add(PosterSlot(path.toBackdropKey(), File(path), path))
            }
        }
    }
    val activePoster = pick.chosenPoster(poster)
    LaunchedEffect(movieId, api) {
        store.measureMissing(movieId, api)
    }
    StackPage(
        title = stringResource(R.string.detail_frames),
        onBack = onBack,
        modifier = modifier,
        scrolling = false,
        headerScrim = headerScrim,
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = grid,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = top + BrandTopInset + 36.dp + 12.dp,
                    bottom = 12.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PosterPicker(
                        posters = posters,
                        active = activePoster,
                        onPick = { key ->
                            tick?.invoke()
                            store.usePoster(movieId, key)
                        },
                        onRemove = { path ->
                            tick?.invoke()
                            store.removePoster(movieId, path)
                        },
                    )
                }
                items(slots.size, key = { slots[it].key }) { index ->
                    val slot = slots[index]
                    val on = pick.enabled(slot.key)
                    val known = pick.width[slot.key]
                    val low = known != null && known < GoodBackdropWidth
                    FrameCell(
                        model = slot.model,
                        on = on,
                        own = slot.path != null,
                        low = low,
                        heldBack = pick.goodOnly && low,
                        onToggle = {
                            tick?.invoke()
                            store.toggle(movieId, slot.key)
                        },
                        onRemove = slot.path?.let { path ->
                            {
                                tick?.invoke()
                                store.removeOwn(movieId, path)
                            }
                        },
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = bottom + 12.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                store.setGoodOnly(movieId, !pick.goodOnly)
                            },
                        )
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = stringResource(R.string.frames_good_only),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.frames_good_only_hint),
                            color = CinemaMuted,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                        )
                    }
                    FrameSwitch(pick.goodOnly)
                }
                Spacer(Modifier.height(8.dp))
                AddPictureButton(stringResource(R.string.frames_poster_add)) {
                    tick?.invoke()
                    addPoster.launch(arrayOf("image/*"))
                }
                Spacer(Modifier.height(8.dp))
                AddPictureButton(stringResource(R.string.frames_add)) {
                    tick?.invoke()
                    add.launch(arrayOf("image/*"))
                }
            }
        }
    }
}

private data class PosterSlot(
    val key: String,
    val model: Any,
    val path: String?,
)

@Composable
private fun PosterPicker(
    posters: List<PosterSlot>,
    active: String?,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            text = stringResource(R.string.frames_poster),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.frames_poster_hint),
            color = CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 17.sp,
        )
        if (posters.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(posters.size, key = { posters[it].key }) { index ->
                    val slot = posters[index]
                    PosterChoice(
                        model = slot.model,
                        selected = slot.key == active,
                        label = stringResource(if (slot.path == null) R.string.frames_poster_api else R.string.frames_own),
                        onPick = { onPick(slot.key) },
                        onRemove = slot.path?.let { path -> { onRemove(path) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterChoice(
    model: Any,
    selected: Boolean,
    label: String,
    onPick: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .width(78.dp)
                .aspectRatio(2f / 3f)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPick,
                ),
        ) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (selected) 1f else 0.55f },
            )
            Text(
                text = label,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
        if (onRemove != null) {
            Text(
                text = stringResource(R.string.frames_remove),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove,
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                color = CinemaMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun AddPictureButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private data class FrameSlot(
    val key: String,
    val model: Any,
    val path: String?,
)

@Composable
private fun FrameCell(
    model: Any,
    on: Boolean,
    own: Boolean,
    low: Boolean,
    heldBack: Boolean,
    onToggle: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.78f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (on && !heldBack) 1f else 0.38f },
        )
        if (own || low) {
            Text(
                text = stringResource(if (low) R.string.frames_low else R.string.frames_own),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (on) Color.White else Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            if (on) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                )
            }
        }
        if (onRemove != null) {
            Text(
                text = stringResource(R.string.frames_remove),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove,
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun FrameSwitch(checked: Boolean) {
    Box(
        Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else Color(0xFF2C2C30))
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color(0xFF8A8A90)),
        )
    }
}
