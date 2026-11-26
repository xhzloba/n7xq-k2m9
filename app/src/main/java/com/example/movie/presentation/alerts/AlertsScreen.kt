package com.example.movie.presentation.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.presentation.common.PosterImage
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.FeedKind
import com.example.movie.domain.model.FeedShelf
import com.example.movie.domain.model.Movie
import com.example.movie.domain.model.SeriesAlert
import com.example.movie.domain.model.kept
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.shell.ShellIcons
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.PosterShape
import java.time.Instant
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun AlertsScreen(
    onMovieClick: (String) -> Unit,
    onMoviePeek: (Movie) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = LocalAppContainer.current.releaseWatch.store
    val alerts by store.alerts.collectAsStateWithLifecycle()
    val shelves by store.shelves.collectAsStateWithLifecycle()
    val feed by store.feed.collectAsStateWithLifecycle()
    val shown = FeedKind.all.mapNotNull { kind ->
        shelves.firstOrNull { it.kind == kind && it.cards.isNotEmpty() && it.kept() }
    }
    var settings by remember { mutableStateOf(false) }
    val tick = rememberHapticTick()
    LaunchedEffect(shelves) { store.markShelvesRead() }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(top = top + 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
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
            Text(
                text = stringResource(R.string.nav_alerts),
                color = Color.White,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(Modifier.weight(1f))
            if (alerts.isNotEmpty() || shown.isNotEmpty()) {
                val context = LocalContext.current
                Text(
                    text = stringResource(R.string.alerts_clear),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                tick?.invoke()
                                store.clearNotices()
                                ReleaseNotifier.dismissAll(context)
                            },
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    color = Color(0xFF4DA3FF),
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = stringResource(R.string.alerts_feed_every, feed.intervalHours),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2C2C30))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            settings = true
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ShellIcons.settings,
                    contentDescription = stringResource(R.string.alerts_settings),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (settings) FeedSheet(onDismiss = { settings = false })
        val sections = noticeSections(shown, alerts)
        if (sections.isEmpty()) {
            Text(
                text = stringResource(R.string.alerts_empty),
                modifier = Modifier.padding(horizontal = PosterGridInset, vertical = 28.dp),
                color = CinemaMuted,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
            ) {
                for (section in sections) {
                    item(key = "head-${section.key}") {
                        Text(
                            text = section.title,
                            modifier = Modifier.padding(
                                start = PosterGridInset,
                                end = PosterGridInset,
                                top = 18.dp,
                                bottom = 2.dp,
                            ),
                            color = CinemaMuted,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            lineHeight = 16.sp,
                        )
                    }
                    items(section.rows, key = { "${section.key}:${it.id}" }) { row ->
                        NoticeLine(row) {
                            store.markMovie(row.id)
                            onMovieClick(row.id)
                        }
                    }
                }
            }
        }
    }
}

private val NoticeOrder = listOf(
    FeedKind.SeriesUpdates,
    FeedKind.MovieUpdates,
    FeedKind.SeriesFresh,
    FeedKind.MovieFresh,
    NoticeQuality,
    NoticeAvailable,
)

private const val NoticeQuality = "quality"
private const val NoticeAvailable = "available"

@Composable
private fun noticeSections(
    shelves: List<FeedShelf>,
    alerts: List<SeriesAlert>,
): List<NoticeSection> {
    val bits = ArrayList<NoticeBit>()
    for (shelf in shelves) {
        val at = if (shelf.at > 0) shelf.at else System.currentTimeMillis()
        for (card in shelf.cards) {
            bits += NoticeBit(
                section = shelf.kind,
                id = card.id,
                title = card.title,
                posterUrl = card.posterUrl,
                at = at,
            )
        }
    }
    for (alert in alerts) {
        bits += NoticeBit(
            section = when {
                alert.quality -> NoticeQuality
                alert.available -> NoticeAvailable
                else -> FeedKind.SeriesUpdates
            },
            id = alert.movieId,
            title = alert.title,
            posterUrl = alert.posterUrl,
            at = alert.at,
            detail = when {
                alert.quality || alert.available -> null
                alert.feed -> stringResource(R.string.alerts_feed)
                else -> stringResource(R.string.alerts_episode, alert.season, alert.episode)
            },
            unread = !alert.read,
        )
    }
    return bits.toSections()
}

@Composable
private fun NoticeLine(row: NoticeRow, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember(row.id) { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = PosterGridInset, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterImage(
            url = row.posterUrl,
            movieId = row.id,
            contentDescription = null,
            modifier = Modifier
                .width(56.dp)
                .height(84.dp)
                .clip(PosterShape)
                .background(Color(0xFF1C1C1E)),
            iconSize = 22.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 10.dp),
        ) {
            Text(
                text = row.title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = row.detail
            if (detail != null) {
                Text(
                    text = detail,
                    modifier = Modifier.padding(top = 2.dp),
                    color = CinemaMuted,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (row.unread) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE23B5A)),
            )
        }
        Text(
            text = row.at.noticeStamp(),
            color = CinemaMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

private data class NoticeBit(
    val section: String,
    val id: String,
    val title: String,
    val posterUrl: String?,
    val at: Long,
    val detail: String? = null,
    val unread: Boolean = false,
)

private data class NoticeRow(
    val id: String,
    val title: String,
    val posterUrl: String?,
    val at: Long,
    val detail: String?,
    val unread: Boolean,
)

private data class NoticeSection(
    val key: String,
    val title: String,
    val rows: List<NoticeRow>,
)

@Composable
private fun List<NoticeBit>.toSections(): List<NoticeSection> =
    groupBy { it.section }
        .entries
        .sortedBy { (key, _) ->
            val index = NoticeOrder.indexOf(key)
            if (index < 0) NoticeOrder.size else index
        }
        .map { (key, items) ->
            val rows = items
                .groupBy { it.id }
                .map { (id, same) ->
                    val latest = same.maxBy { it.at }
                    NoticeRow(
                        id = id,
                        title = latest.title,
                        posterUrl = same.firstNotNullOfOrNull { it.posterUrl },
                        at = latest.at,
                        detail = same.firstNotNullOfOrNull { it.detail },
                        unread = same.any { it.unread },
                    )
                }
                .sortedByDescending { it.at }
            NoticeSection(key, stringResource(sectionTitle(key)), rows)
        }

private fun sectionTitle(key: String): Int = when (key) {
    NoticeQuality -> R.string.alerts_sec_quality
    NoticeAvailable -> R.string.alerts_available
    else -> categoryTitle(key)
}

private fun Long.noticeStamp(): String {
    val moment = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
    val date = moment.toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> moment.format(DateTimeFormatter.ofPattern("HH:mm"))
        today.minusDays(1) -> "вчера"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru")))
    }
}
