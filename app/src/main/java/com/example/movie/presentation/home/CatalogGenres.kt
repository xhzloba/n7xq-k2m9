package com.example.movie.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.haptic.HapticOnChange
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.home.components.RailTitle
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.presentation.shell.ShellIcons
import java.util.Locale

internal const val GenreAll = "Все"

internal val CatalogGenres = listOf(
    GenreAll,
    "аниме",
    "биография",
    "боевик",
    "вестерн",
    "военный",
    "детектив",
    "детский",
    "для взрослых",
    "документальный",
    "драма",
    "игра",
    "история",
    "комедия",
    "концерт",
    "короткометражка",
    "криминал",
    "мелодрама",
    "музыка",
    "мультфильм",
    "мюзикл",
    "приключения",
    "семейный",
    "спорт",
    "ток-шоу",
    "триллер",
    "ужасы",
    "фантастика",
    "фильм-нуар",
    "фэнтези",
)

/** В «Фильмы» аниме не показываем — у него своя вкладка. */
internal fun catalogGenresFor(type: String): List<String> =
    if (type == "movie") CatalogGenres.filter { it != "аниме" } else CatalogGenres

/** В слайдере чипсов нет. В полном списке жанров остаются. */
private val SliderHidden = setOf(
    "военный",
    "детский",
    "для взрослых",
    "документальный",
    "игра",
    "концерт",
    "мюзикл",
    "ток-шоу",
    "фильм-нуар",
)

private fun genreMark(name: String): Int? = when (name) {
    "аниме" -> R.drawable.genre_anime
    "биография" -> R.drawable.genre_biography
    "боевик" -> R.drawable.genre_action
    "вестерн" -> R.drawable.genre_western
    "детектив" -> R.drawable.genre_detective
    "драма" -> R.drawable.genre_drama
    "история" -> R.drawable.genre_historical
    "комедия" -> R.drawable.genre_comedy
    "короткометражка" -> R.drawable.genre_short
    "криминал" -> R.drawable.genre_crime
    "мелодрама" -> R.drawable.genre_melodrama
    "музыка" -> R.drawable.genre_music
    "мультфильм" -> R.drawable.genre_cartoon
    "приключения" -> R.drawable.genre_adventure
    "семейный" -> R.drawable.genre_family
    "спорт" -> R.drawable.genre_sport
    "триллер" -> R.drawable.genre_thriller
    "ужасы" -> R.drawable.genre_horror
    "фантастика" -> R.drawable.genre_scifi
    "фэнтези" -> R.drawable.genre_fantasy
    else -> null
}

// Как в Plex: stadium-пилюля, тёмный fill, без обводки. Жанры с картинкой — сама подпись, без пилюли.
private val GenreChipShape = RoundedCornerShape(percent = 50)
private val GenreChipCinema = Color(0xFF262626)
private val GenreChipGraphite = Color(0xFF2A2E38)

@Composable
internal fun CatalogGenreChips(
    type: String,
    onOpenAll: () -> Unit,
    onGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val fill = if (settings.appTheme == THEME_GRAPHITE) GenreChipGraphite else GenreChipCinema
    val genres = remember(type) {
        catalogGenresFor(type).filter { it != GenreAll && it !in SliderHidden }
    }
    val row = rememberLazyListState()
    HapticOnChange(row) { row.firstVisibleItemIndex }
    Column(modifier.fillMaxWidth()) {
        RailTitle(
            title = stringResource(R.string.catalog_genres),
            onClick = onOpenAll,
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            state = row,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = PosterGridInset),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(genres, key = { it }) { name ->
                GenreChip(
                    name = name,
                    fill = fill,
                    onClick = { onGenre(name) },
                )
            }
        }
    }
}

@Composable
private fun GenreChip(
    name: String,
    fill: Color,
    onClick: () -> Unit,
) {
    val tick = rememberHapticTick()
    var pressed by remember(name) { mutableStateOf(false) }
    val scale = rememberPressScale(pressed)
    val wave = remember(name) { PosterWave() }
    val mark = genreMark(name)
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (mark == null) {
                    Modifier
                        .clip(GenreChipShape)
                        .background(fill)
                } else {
                    Modifier
                },
            )
            .posterWave(wave, light = true)
            .pressToOpen(
                onPressed = { pressed = it },
                onClick = {
                    tick?.invoke()
                    onClick()
                },
                wave = wave,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (mark == null) {
            Text(
                text = name.replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale("ru")) else ch.toString()
                },
                color = Color.White,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        } else {
            val painter = painterResource(mark)
            val size = painter.intrinsicSize
            val aspect = if (size.height > 0f) size.width / size.height else 2.4f
            Image(
                painter = painter,
                contentDescription = name,
                modifier = Modifier
                    .height(if (name == "вестерн" || name == "боевик") 32.dp else 44.dp)
                    .aspectRatio(aspect),
            )
        }
    }
}

@Composable
internal fun CatalogGenresPage(
    type: String,
    onBack: () -> Unit,
    onGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val header = top + BrandTopInset + 36.dp + 18.dp
    val genres = remember(type) { catalogGenresFor(type) }
    val grid = rememberLazyGridState()
    val density = LocalDensity.current
    val scrim = if (grid.firstVisibleItemIndex > 0) {
        1f
    } else {
        val threshold = with(density) { 28.dp.toPx() }
        (grid.firstVisibleItemScrollOffset / threshold).coerceIn(0f, 1f)
    }
    Box(
        modifier
            .fillMaxSize()
            .appBackground()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
    ) {
        LazyVerticalGrid(
            state = grid,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = PosterGridInset,
                end = PosterGridInset,
                top = header,
                bottom = 24.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(genres, key = { it }) { name ->
                GenreTile(name = name, onClick = { onGenre(name) })
            }
        }
        // Та же растушёвка шапки, что у полного списка (cinema / graphite).
        SectionScrim(progress = scrim, modifier = Modifier.align(Alignment.TopCenter))
        GenresBar(onBack = onBack, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun GenreTile(name: String, onClick: () -> Unit) {
    val tick = rememberHapticTick()
    var pressed by remember(name) { mutableStateOf(false) }
    val scale = rememberPressScale(pressed)
    val wave = remember(name) { PosterWave() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .posterWave(wave, light = true)
            .pressToOpen(
                onPressed = { pressed = it },
                onClick = {
                    tick?.invoke()
                    onClick()
                },
                wave = wave,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun GenresBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberHapticTick()
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .padding(top = BrandTopInset)
            .height(36.dp),
    ) {
        Text(
            text = stringResource(R.string.catalog_genres),
            color = Color.White,
            fontSize = 18.sp,
            lineHeight = 24.sp,
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
