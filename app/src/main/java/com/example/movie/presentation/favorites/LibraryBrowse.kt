package com.example.movie.presentation.favorites

import java.text.Collator
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.presentation.home.CoverPopup
import com.example.movie.R
import com.example.movie.presentation.common.PosterImage
import com.example.movie.presentation.detail.DetailSeed
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.SavedTitle
import com.example.movie.domain.model.toMovie
import com.example.movie.domain.model.HapticSlot
import com.example.movie.presentation.haptic.rememberHaptic
import com.example.movie.presentation.home.components.PosterCaption
import com.example.movie.presentation.home.components.WatchedBadge
import com.example.movie.presentation.home.components.WatchedBadgeHang
import com.example.movie.presentation.home.components.rememberWatchedIds
import com.example.movie.presentation.home.components.WatchProgress
import com.example.movie.presentation.home.holdCue
import com.example.movie.presentation.home.holdToPeek
import com.example.movie.presentation.home.PosterWave
import com.example.movie.presentation.home.posterWave
import com.example.movie.presentation.home.rememberPressScale
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PosterShape

internal fun List<SavedTitle>.sortedFor(sort: LibrarySort?): List<SavedTitle> = when (sort) {
    null -> this
    LibrarySort.Title -> sortedWith(
        compareBy(Collator.getInstance(Locale.forLanguageTag("ru"))) { it.title },
    )
    LibrarySort.Year -> sortedWith(
        compareByDescending<SavedTitle> { it.year != null }
            .thenByDescending { it.year ?: 0 }
            .thenBy { it.title.lowercase() },
    )
    LibrarySort.Rating -> sortedWith(
        compareByDescending<SavedTitle> { it.ratingKp != null || it.ratingImdb != null }
            .thenByDescending { maxOf(it.ratingKp ?: 0f, it.ratingImdb ?: 0f) }
            .thenBy { it.title.lowercase() },
    )
    LibrarySort.Release -> sortedWith(
        compareByDescending<SavedTitle> { !it.releaseDate.isNullOrBlank() }
            .thenByDescending { it.releaseDate.orEmpty() }
            .thenBy { it.title.lowercase() },
    )
}

@Composable
internal fun LibraryOptionSheet(
    title: String,
    options: List<Pair<String, String>>,
    selected: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tick = rememberHaptic(HapticSlot.Menu)
    CoverPopup(onDismiss = onDismiss) {
        Text(
            text = title,
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
        options.forEach { (id, label) ->
            val on = id == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember(id) { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onPick(id)
                        },
                    )
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    color = if (on) Color.White else CinemaMuted,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = if (on) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (on) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
internal fun FavoritePoster(
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
    val watched = rememberWatchedIds()
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
            if (item.id in watched) {
                WatchedBadge(Modifier.align(Alignment.BottomEnd))
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
        PosterCaption(item.title, item.year, item.releaseDate)
    }
}

