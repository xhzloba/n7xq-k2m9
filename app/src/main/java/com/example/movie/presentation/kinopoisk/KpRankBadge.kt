package com.example.movie.presentation.kinopoisk

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.data.kinopoisk.KpRank
import com.example.movie.presentation.common.appChrome

@Composable
private fun BoxOfficePlate(gross: String) {
    val description = stringResource(R.string.kp_box_office, gross)
    Box(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
    ) {
        Text(
            text = gross,
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            style = TextStyle(shadow = RankShadow),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(5.dp)
                .shadow(8.dp, RoundedCornerShape(5.dp), clip = false)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

private val RankUp = Color(0xFF3DDC84)
private val RankDown = Color(0xFFFF5A5A)

private val RankNotch = RoundedCornerShape(
    topStart = 0.dp,
    bottomStart = 0.dp,
    topEnd = 7.dp,
    bottomEnd = 7.dp,
)

private val RankShadow = Shadow(
    color = Color.Black.copy(alpha = 0.9f),
    offset = Offset(0f, 1f),
    blurRadius = 12f,
)

@Composable
internal fun KpRankOverlay(rank: KpRank) {
    val gross = rank.boxOffice
    if (gross != null) {
        BoxOfficePlate(gross)
        return
    }
    val description = when {
        rank.up == true && rank.diff != null -> stringResource(R.string.kp_rank_up, rank.position, rank.diff)
        rank.up == false && rank.diff != null -> stringResource(R.string.kp_rank_down, rank.position, rank.diff)
        else -> stringResource(R.string.kp_rank, rank.position)
    }
    Box(
        Modifier
            .fillMaxSize()
            .semantics { contentDescription = description },
    ) {
        val diff = rank.diff
        val up = rank.up
        Row(
            Modifier
                .align(Alignment.CenterStart)
                .clip(RankNotch)
                .background(appChrome())
                .padding(start = 4.dp, end = 7.dp, top = 1.dp, bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (diff != null && up != null) {
                Text(
                    text = if (up) "▲$diff" else "▼$diff",
                    color = if (up) RankUp else RankDown,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Text(
                text = rank.position.toString(),
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}
