package com.example.movie.tvui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.movie.R
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.Movie
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.PosterShape
import kotlinx.coroutines.delay

private val FieldShape = RoundedCornerShape(percent = 50)

@Composable
internal fun TvSearchScreen(
    menuFocus: FocusRequester,
    posterTarget: TvPosterTarget,
    menuFocused: Boolean,
    onReachMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var failed by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val posters = remember(results) { List(results.size) { FocusRequester() } }
    var landed by remember { mutableStateOf<FocusRequester?>(null) }
    SideEffect {
        val back = landed ?: field
        posterTarget.requester = back
        posterTarget.poster = back
    }
    LaunchedEffect(query) {
        val text = query.trim()
        if (text.length < 2) {
            results = emptyList()
            failed = false
            return@LaunchedEffect
        }
        delay(280)
        val page = container.searchMovies(text)
        if (query.trim() != text) return@LaunchedEffect
        page.onSuccess {
            results = it.movies
            failed = false
        }.onFailure {
            results = emptyList()
            failed = true
        }
    }
    LaunchedEffect(menuFocused) {
        if (menuFocused) return@LaunchedEffect
        runCatching { (landed ?: field).requestFocus() }
    }
    Column(
        modifier
            .fillMaxSize()
            .padding(start = 56.dp, top = 48.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
            cursorBrush = SolidColor(Color(0xFF2AD8FF)),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 18.sp,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .width(560.dp)
                .height(48.dp)
                .focusRequester(field)
                .onFocusChanged {
                    if (it.isFocused) {
                        landed = field
                        keyboard?.show()
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                        return@onPreviewKeyEvent false
                    }
                    val native = event.nativeKeyEvent
                    if (native.repeatCount > 0 || native.isLongPress) return@onPreviewKeyEvent true
                    onReachMenu()
                    true
                }
                .focusProperties {
                    left = FocusRequester.Cancel
                    up = FocusRequester.Cancel
                    right = FocusRequester.Cancel
                    down = posters.firstOrNull() ?: FocusRequester.Cancel
                }
                .clip(FieldShape)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.height(28.dp))
        when {
            failed -> Text(
                text = stringResource(R.string.popular_load_error),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
            )
            query.trim().length >= 2 && results.isEmpty() -> Text(
                text = stringResource(R.string.catalog_empty),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
            )
            results.isNotEmpty() -> LazyRow(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(end = 48.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
            ) {
                itemsIndexed(results, key = { index, item -> "${item.id}#$index" }) { index, item ->
                    val focus = posters[index]
                    var focused by remember(item.id) { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .width(128.dp)
                            .height(188.dp)
                            .focusRequester(focus)
                            .onFocusChanged {
                                focused = it.isFocused
                                if (it.isFocused) landed = focus
                            }
                            .onPreviewKeyEvent { event ->
                                if (index != 0 || !focused) return@onPreviewKeyEvent false
                                if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionLeft) {
                                    return@onPreviewKeyEvent false
                                }
                                val native = event.nativeKeyEvent
                                if (native.repeatCount > 0 || native.isLongPress) return@onPreviewKeyEvent true
                                onReachMenu()
                                true
                            }
                            .focusProperties {
                                left = if (index == 0) FocusRequester.Cancel else FocusRequester.Default
                                right = if (index == results.lastIndex) FocusRequester.Cancel else FocusRequester.Default
                                up = field
                                down = FocusRequester.Cancel
                            }
                            .border(
                                width = 2.dp,
                                color = if (focused) Color.White else Color.Transparent,
                                shape = PosterShape,
                            )
                            .clip(PosterShape)
                            .background(Color(0xFF2A2A2E))
                            .clickable(
                                interactionSource = remember(item.id) { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val cover = item.posterUrl?.takeIf { it.isNotBlank() }
                        if (cover != null) {
                            AsyncImage(
                                model = cover,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = ShellIcons.trailers,
                                contentDescription = item.title,
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
