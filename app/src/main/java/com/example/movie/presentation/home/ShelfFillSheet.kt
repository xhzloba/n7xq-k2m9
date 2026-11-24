package com.example.movie.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.presentation.detail.drawPlexWash
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.palette.PosterWash
import com.example.movie.ui.theme.CinemaMuted
import kotlin.math.max
import kotlin.math.min

/** 0 — без заливки, не пишется. 1 — UltraBlur с первого постера полки. Остальное — ARGB. */
internal const val ShelfFillUltra = 1

/** Столбик из трёх ступеней: сверху самая глухая, ниже насыщеннее. Яркие ряды не показываем. */
internal val ShelfFillColumns: List<List<Int>> = buildList {
    add(
        listOf(
            0xFF101012.toInt(),
            0xFF1C1C1E.toInt(),
            0xFF3A3A3C.toInt(),
        ),
    )
    val hues = floatArrayOf(
        4f, 18f, 32f, 46f, 58f, 78f, 105f, 140f, 164f,
        184f, 204f, 222f, 246f, 268f, 288f, 312f, 334f, 350f,
    )
    val steps = arrayOf(
        0.62f to 0.14f,
        0.78f to 0.26f,
        0.86f to 0.40f,
    )
    for (hue in hues) {
        add(steps.map { (saturation, lightness) -> Color.hsl(hue, saturation, lightness).toArgb() })
    }
}

/** Цветная или светлая заливка: год под названием белый. Тёмно-серые оставляют серый год. */
internal fun Int.shelfYearWhite(): Boolean {
    val color = Color(this)
    val red = color.red
    val green = color.green
    val blue = color.blue
    val peak = max(red, max(green, blue))
    val floor = min(red, min(green, blue))
    val saturation = if (peak == 0f) 0f else (peak - floor) / peak
    return saturation >= 0.22f || color.luminance() >= 0.28f
}

@Composable
internal fun ShelfFillSheet(
    title: String,
    color: Int,
    wash: PosterWash?,
    onColor: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val tick = rememberHapticTick()
    val ultra = color == ShelfFillUltra
    CoverPopup(onDismiss = onDismiss) {
        Text(
            text = title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.shelf_fill_color),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            color = CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShelfFillDot(
                    color = null,
                    selected = color == 0,
                    onClick = {
                        tick?.invoke()
                        onColor(0)
                    },
                )
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            tick?.invoke()
                            onColor(ShelfFillUltra)
                        },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShelfUltraDot(wash = wash, selected = ultra)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.shelf_fill_ultra),
                        color = if (ultra) Color.White else CinemaMuted,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                    )
                }
            }
            ShelfFillColumns.forEach { column ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    column.forEach { tag ->
                        ShelfFillDot(
                            color = Color(tag),
                            selected = color == tag,
                            onClick = {
                                tick?.invoke()
                                onColor(tag)
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private val UltraMark = PosterWash(
    topLeft = Color(0xFF143A73),
    topRight = Color(0xFF5A1E4A),
    bottomRight = Color(0xFF0E4A45),
    bottomLeft = Color(0xFF1A2744),
)

@Composable
private fun ShelfUltraDot(
    wash: PosterWash?,
    selected: Boolean,
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .drawBehind { drawPlexWash(wash ?: UltraMark, 1f) },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.shelf_fill_ultra),
                tint = Color.White,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun ShelfFillDot(
    color: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink = if (color != null && color.luminance() > 0.62f) Color.Black else Color.White
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .then(
                if (color == null) {
                    Modifier.border(1.5.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                } else {
                    Modifier.background(color)
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.shelf_fill_none),
                tint = ink,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}
