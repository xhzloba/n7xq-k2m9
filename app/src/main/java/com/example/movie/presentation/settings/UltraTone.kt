package com.example.movie.presentation.settings

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.movie.R
import com.example.movie.presentation.detail.graded
import com.example.movie.presentation.palette.PosterWash
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val ToneCorners = intArrayOf(0, 1, 3, 2)

@Composable
internal fun WashToneButton(
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val r = size.minDimension * 0.18f
            val spots = arrayOf(
                Offset(size.width * 0.28f, size.height * 0.28f),
                Offset(size.width * 0.72f, size.height * 0.28f),
                Offset(size.width * 0.28f, size.height * 0.72f),
                Offset(size.width * 0.72f, size.height * 0.72f),
            )
            val paint = intArrayOf(0, 1, 3, 2)
            spots.forEachIndexed { index, at ->
                drawCircle(
                    color = colors.getOrElse(paint[index]) { Color.White },
                    radius = r,
                    center = at,
                )
            }
        }
    }
}

@Composable
internal fun WashToneSheet(
    movieId: String,
    wash: PosterWash,
    backdropUrl: String?,
    previewOn: Boolean,
    onPreview: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val store = com.example.movie.di.LocalAppContainer.current.washTones
    val tones by store.tones.collectAsStateWithLifecycle()
    val tone = tones[movieId] ?: com.example.movie.data.palette.WashTone()
    val shown = wash.graded(tone.lights, tone.punch, tone.pick)
    val corners = listOf(shown.topLeft, shown.topRight, shown.bottomRight, shown.bottomLeft)
    val raw = listOf(wash.topLeft, wash.topRight, wash.bottomRight, wash.bottomLeft)
    var open by remember { mutableIntStateOf(0) }
    val names = listOf(
        stringResource(R.string.settings_plex_tl),
        stringResource(R.string.settings_plex_tr),
        stringResource(R.string.settings_plex_br),
        stringResource(R.string.settings_plex_bl),
    )
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        Column(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 62.dp, end = 10.dp)
                .width(292.dp)
                .heightIn(max = 540.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black)
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_plex_tone_title),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            PreviewSwitch(checked = previewOn, onChecked = onPreview)
            Spacer(Modifier.height(8.dp))
            BackdropDropper(
                url = backdropUrl,
                onPick = { color ->
                    val index = open.coerceIn(0, 3)
                    store.setPick(movieId, index, color)
                },
            )
            Spacer(Modifier.height(8.dp))
            CornerGrid(
                names = names,
                colors = corners,
                selected = open,
                onSelect = { open = it },
            )
            Text(
                text = stringResource(R.string.settings_plex_copy),
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
            )
            Row(Modifier.fillMaxWidth()) {
                ToneCorners.filter { it != open }.forEach { index ->
                    Text(
                        text = names[index],
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    store.copyCorner(movieId, open, index, raw[open].toArgb())
                                },
                            )
                            .padding(vertical = 8.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            CornerTone(
                punch = tone.punch.getOrElse(open) { 0f },
                light = tone.lights.getOrElse(open) { 0f },
                picked = tone.pick.getOrElse(open) { 0 } != 0,
                onPunch = { store.setPunch(movieId, open, it) },
                onLight = { store.setLight(movieId, open, it) },
                onHue = { hue ->
                    val current = tone.pick.getOrElse(open) { 0 }
                    val sat = if (current != 0) Color(current).sat() else raw[open].sat()
                    store.setPick(movieId, open, raw[open].picked(hue, sat))
                },
                onSat = { sat ->
                    val current = tone.pick.getOrElse(open) { 0 }
                    val hue = if (current != 0) Color(current).hue() else raw[open].hue()
                    store.setPick(movieId, open, raw[open].picked(hue, sat))
                },
                hue = run {
                    val pick = tone.pick.getOrElse(open) { 0 }
                    if (pick != 0) Color(pick).hue() else raw[open].hue()
                },
                sat = run {
                    val pick = tone.pick.getOrElse(open) { 0 }
                    if (pick != 0) Color(pick).sat() else raw[open].sat()
                },
                onClear = { store.setPick(movieId, open, 0) },
            )
            Spacer(Modifier.height(8.dp))
            ToneSlider(
                title = stringResource(R.string.settings_plex_intensity),
                hint = stringResource(R.string.settings_plex_intensity_hint),
                value = tone.intensity,
                range = 0.45f..1.55f,
                label = "${(tone.intensity * 100).roundToInt()}",
                onChange = { store.setIntensity(movieId, it) },
            )
            Text(
                text = stringResource(R.string.settings_plex_tone_reset),
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { store.reset(movieId) },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CornerTone(
    punch: Float,
    light: Float,
    picked: Boolean,
    onPunch: (Float) -> Unit,
    onLight: (Float) -> Unit,
    onHue: (Float) -> Unit,
    onSat: (Float) -> Unit,
    hue: Float,
    sat: Float,
    onClear: () -> Unit,
) {
    ToneSlider(
        title = stringResource(R.string.settings_plex_punch),
        hint = stringResource(R.string.settings_plex_punch_hint),
        value = punch,
        range = -1f..1f,
        label = "",
        onChange = onPunch,
    )
    ToneSlider(
        title = stringResource(R.string.settings_plex_light),
        hint = stringResource(R.string.settings_plex_light_hint),
        value = light,
        range = -1f..1f,
        label = "${(light * 100).roundToInt()}",
        onChange = onLight,
    )
    ToneSlider(
            title = stringResource(R.string.settings_plex_hue),
            hint = stringResource(R.string.settings_plex_hue_hint),
            value = hue,
            range = 0f..360f,
            label = "${hue.roundToInt()}",
            onChange = onHue,
        )
        ToneSlider(
            title = stringResource(R.string.settings_plex_sat),
            hint = stringResource(R.string.settings_plex_sat_hint),
            value = sat,
            range = 0f..1f,
            label = "${(sat * 100).roundToInt()}",
            onChange = onSat,
        )
        if (picked) {
            Text(
                text = stringResource(R.string.settings_plex_pick_clear),
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClear,
                    )
                    .padding(bottom = 8.dp),
                color = Color.White,
                fontSize = 13.sp,
            )
        }
}

@Composable
private fun CornerGrid(
    names: List<String>,
    colors: List<Color>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth()) {
        ToneCorners.take(2).forEach { index ->
            CornerChip(names[index], colors[index], index == selected, Modifier.weight(1f)) {
                onSelect(index)
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth()) {
        ToneCorners.drop(2).forEach { index ->
            CornerChip(names[index], colors[index], index == selected, Modifier.weight(1f)) {
                onSelect(index)
            }
        }
    }
}

@Composable
private fun CornerChip(
    name: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .padding(end = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color.White.copy(alpha = 0.16f) else Color.Transparent)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.22f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        )
        Text(
            text = name,
            modifier = Modifier.padding(start = 8.dp),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun ToneSlider(
    title: String,
    hint: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    onChange: (Float) -> Unit,
) {
    if (title.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            if (label.isNotEmpty()) {
                Text(text = label, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }
    }
    Slider(
        value = value.coerceIn(range),
        onValueChange = onChange,
        valueRange = range,
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.White.copy(alpha = 0.28f),
        ),
    )
    if (hint.isNotEmpty()) {
        Text(
            text = hint,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 12.sp,
            lineHeight = 15.sp,
        )
    }
}

private fun Color.hue(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv[0]
}

private fun Color.sat(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv[1]
}

private fun Color.picked(hue: Float, sat: Float): Int {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[0] = hue
    hsv[1] = sat.coerceIn(0f, 1f)
    hsv[2] = hsv[2].coerceIn(0.12f, 0.72f)
    return android.graphics.Color.HSVToColor(255, hsv)
}

@Composable
private fun PreviewSwitch(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onChecked(!checked) },
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = stringResource(R.string.settings_plex_preview),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.settings_plex_preview_hint),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 12.sp,
                lineHeight = 15.sp,
            )
        }
        Box(
            Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (checked) Color.White else Color.White.copy(alpha = 0.22f))
                .padding(3.dp),
        ) {
            Box(
                Modifier
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color.Black else Color.White),
            )
        }
    }
}

@Composable
private fun BackdropDropper(
    url: String?,
    onPick: (Int) -> Unit,
) {
    val context = LocalContext.current
    var frame by remember(url) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(url) {
        frame = null
        failed = false
        if (url.isNullOrBlank()) return@LaunchedEffect
        val result = withContext(Dispatchers.IO) {
            context.imageLoader.execute(
                ImageRequest.Builder(context)
                    .data(stillSource(url))
                    .allowHardware(false)
                    .size(720)
                    .build(),
            )
        }
        val bitmap = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        if (bitmap == null) failed = true else frame = bitmap
    }
    val bitmap = frame
    when {
        url.isNullOrBlank() -> DropperNote(stringResource(R.string.settings_plex_dropper_empty))
        failed -> DropperNote(stringResource(R.string.settings_plex_dropper_fail))
        bitmap == null -> DropperNote(stringResource(R.string.settings_plex_dropper_hint))
        else -> {
            DropperNote(stringResource(R.string.settings_plex_dropper_hint))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .pointerInput(bitmap) {
                        detectTapGestures { tap ->
                            val color = sampleCrop(bitmap, tap, size)
                            if (color ushr 24 != 0) onPick(color)
                        }
                    },
            )
        }
    }
}

@Composable
private fun DropperNote(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.72f),
        fontSize = 12.sp,
        lineHeight = 15.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

private fun stillSource(url: String): Any {
    if (!url.startsWith("file:")) return url
    return runCatching { java.io.File(java.net.URI(url)) }.getOrDefault(url)
}

private fun sampleCrop(bitmap: Bitmap, tap: Offset, view: IntSize): Int {
    val srcW = bitmap.width.toFloat()
    val srcH = bitmap.height.toFloat()
    val dstW = view.width.toFloat()
    val dstH = view.height.toFloat()
    if (srcW < 2f || srcH < 2f || dstW < 2f || dstH < 2f) return 0
    val scale = max(dstW / srcW, dstH / srcH)
    val offsetX = (dstW - srcW * scale) / 2f
    val x = ((tap.x - offsetX) / scale).toInt().coerceIn(0, bitmap.width - 1)
    val y = (tap.y / scale).toInt().coerceIn(0, bitmap.height - 1)
    return bitmap.getPixel(x, y)
}
