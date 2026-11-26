package com.example.movie.presentation.splash

import android.graphics.BitmapFactory
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.BuildConfig
import com.example.movie.R
import com.example.movie.presentation.tv.dpadSkip
import com.example.movie.presentation.common.appBackground
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaMuted
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val LogoAspect = 915f / 203f
private val Enter = CubicBezierEasing(0.22f, 0.08f, 0.18f, 1f)

private fun Vibrator.splashBuzz() {
    if (!hasVibrator()) return
    val effect = if (hasAmplitudeControl()) {
        VibrationEffect.createWaveform(longArrayOf(0, 90, 110), intArrayOf(0, 72, 48), 0)
    } else {
        VibrationEffect.createWaveform(longArrayOf(0, 50, 150), 0)
    }
    vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION))
}

@Composable
internal fun SplashScreen(onReveal: () -> Unit, onFinished: () -> Unit) {
    val enter = remember { Animatable(0f) }
    val version = remember { Animatable(0f) }
    val veil = remember { Animatable(1f) }
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val vibrator = remember {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }
    LaunchedEffect(Unit) {
        if (settings.haptic) vibrator?.splashBuzz()
        try {
            enter.animateTo(1f, tween(820, easing = Enter))
            version.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            delay(500)
            onReveal()
            delay(200)
            veil.animateTo(0f, tween(640, easing = FastOutSlowInEasing))
            onFinished()
        } finally {
            vibrator?.cancel()
        }
    }
    BackHandler(enabled = true) {}
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = veil.value)
            .appBackground()
            .dpadSkip()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        val mark = maxWidth
        SplashWordmark(
            enter = enter.value,
            width = mark * 0.58f,
            slide = mark * 0.5f,
        )
        Text(
            text = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = version.value },
            color = CinemaMuted,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun SplashWordmark(
    enter: Float,
    width: Dp,
    slide: Dp,
) {
    val context = LocalContext.current
    val logo = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.lumen_logo, options)?.asImageBitmap()
    } ?: return
    val slidePx = with(LocalDensity.current) { slide.toPx() }
    Canvas(
        modifier = Modifier
            .width(width)
            .aspectRatio(LogoAspect)
            .graphicsLayer {
                translationX = (1f - enter) * -slidePx
                alpha = (enter / 0.16f).coerceIn(0f, 1f)
            }
            .semantics { contentDescription = "Люмен" },
    ) {
        drawImage(
            image = logo,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.High,
        )
    }
}
