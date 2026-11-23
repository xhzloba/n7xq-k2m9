package com.example.movie.presentation.favorites

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.domain.model.DefaultLibraryOrder
import com.example.movie.domain.model.LibraryShelves
import com.example.movie.domain.model.canMoveLibraryShelf
import com.example.movie.domain.model.moveLibraryShelf
import com.example.movie.domain.model.normalizedLibraryOrder
import com.example.movie.presentation.common.BookmarkFilled
import com.example.movie.presentation.common.PlexPlay
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.CornerIris
import com.example.movie.presentation.home.CoverPopup
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.HomeContentInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import androidx.compose.ui.geometry.Offset

private data class ShelfRow(
    val id: String,
    @StringRes val title: Int,
    val icon: ImageVector?,
)

private val Shelves = listOf(
    ShelfRow(LibraryShelves.Favorites, R.string.section_favorites, BookmarkFilled),
    ShelfRow(LibraryShelves.Continue, R.string.section_continue, PlexPlay),
    ShelfRow(LibraryShelves.History, R.string.section_history, ShellIcons.history),
    ShelfRow(LibraryShelves.Watched, R.string.library_watched, Icons.Filled.CheckCircle),
    ShelfRow(LibraryShelves.Downloads, R.string.library_downloads, null),
    ShelfRow(LibraryShelves.Folders, R.string.library_folders, FolderMark),
)

private fun orderPanel(theme: String): Color =
    if (theme == THEME_GRAPHITE) Color(0xFF22262E) else Color.White.copy(alpha = 0.08f)

/**
 * Долгий тап по «Моё»: тот же круг, что у «Главной», только порядок полок.
 */
@Composable
internal fun LibraryOrderCover(
    visible: Boolean,
    color: Color,
    onAlive: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            onAlive(true)
            progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        } else if (shown) {
            progress.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
            shown = false
            onAlive(false)
        }
    }
    if (!shown) return
    BackHandler(enabled = visible) { onDismiss() }
    val p = progress.value
    val contentAlpha = ((p - 0.78f) / 0.22f).coerceIn(0f, 1f)
    val navPad = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize()) {
        CornerIris(progress = p, color = color, modifier = Modifier.fillMaxSize())
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
                    Text(
                        text = stringResource(R.string.library_order_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
                        letterSpacing = (-0.3).sp,
                        modifier = Modifier.weight(1f),
                    )
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = navPad),
                    ) {
                        LibraryOrderBody()
                    }
                }
            }
        }
    }
}

/** Тот же список, что в круге, но плашкой снизу — как «Вид полок» на главной. */
@Composable
internal fun LibraryOrderSheet(onDismiss: () -> Unit) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val panel = orderPanel(settings.appTheme)
    val sheetCap = (LocalConfiguration.current.screenHeightDp * 0.58f).dp
    val listCap = sheetCap - 64.dp
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
                text = stringResource(R.string.library_order_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.3).sp,
                modifier = Modifier.padding(horizontal = 22.dp),
            )
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = listCap)
                    .padding(horizontal = PosterGridInset)
                    .nestedScroll(keepSheet)
                    .clip(RoundedCornerShape(20.dp))
                    .background(panel)
                    .verticalScroll(rememberScrollState()),
            ) {
                LibraryOrderBody()
            }
        }
    }
}

@Composable
private fun LibraryOrderBody() {
    val repository = LocalUserSettings.current
    val settings by repository.settings.collectAsStateWithLifecycle()
    val order = settings.libraryOrder.normalizedLibraryOrder()
    val atDefault = order == DefaultLibraryOrder
    val tick = rememberHapticTick()
    val byId = Shelves.associateBy { it.id }
    Text(
        text = stringResource(R.string.library_order_note),
        color = CinemaMuted,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
    Text(
        text = stringResource(R.string.home_rails_reset),
        color = if (atDefault) CinemaMuted else Color.White,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        textDecoration = if (atDefault) null else TextDecoration.Underline,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !atDefault,
                onClick = {
                    tick?.invoke()
                    repository.setLibraryOrder(DefaultLibraryOrder)
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
    OrderDivider()
    order.forEachIndexed { index, id ->
        val row = byId[id] ?: return@forEachIndexed
        if (index > 0) OrderDivider()
        OrderRow(
            title = stringResource(row.title),
            icon = row.icon,
            upEnabled = order.canMoveLibraryShelf(id, up = true),
            downEnabled = order.canMoveLibraryShelf(id, up = false),
            onUp = { repository.setLibraryOrder(order.moveLibraryShelf(id, up = true)) },
            onDown = { repository.setLibraryOrder(order.moveLibraryShelf(id, up = false)) },
        )
    }
}

@Composable
private fun OrderDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    )
}

@Composable
private fun OrderRow(
    title: String,
    icon: ImageVector?,
    upEnabled: Boolean,
    downEnabled: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val tick = rememberHapticTick()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrderNudge(up = true, enabled = upEnabled, onClick = {
            tick?.invoke()
            onUp()
        })
        OrderNudge(up = false, enabled = downEnabled, onClick = {
            tick?.invoke()
            onDown()
        })
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Spacer(Modifier.width(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun OrderNudge(up: Boolean, enabled: Boolean, onClick: () -> Unit) {
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
