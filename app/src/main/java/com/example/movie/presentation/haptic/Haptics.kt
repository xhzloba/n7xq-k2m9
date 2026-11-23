package com.example.movie.presentation.haptic

import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.domain.model.HapticFeel
import com.example.movie.domain.model.HapticSlot
import com.example.movie.domain.model.hapticFeel
import com.example.movie.presentation.settings.LocalUserSettings
import kotlin.math.abs

/** @hide в SDK, значения стабильны с API 31. */
private const val EFFECT_THUD = 3
private const val EFFECT_POP = 4
private const val EFFECT_TEXTURE_TICK = 21

@Composable
internal fun rememberHapticTick(): (() -> Unit)? = rememberHaptic(HapticSlot.Menu)

@Composable
internal fun rememberHaptic(slot: HapticSlot): (() -> Unit)? {
    val vibrator = rememberVibrator()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val feel = if (settings.haptic) settings.hapticProfile.hapticFeel(slot) else HapticFeel.Off
    val current = rememberUpdatedState(feel)
    val play = remember(vibrator) {
        vibrator?.let { motor -> { motor.play(current.value) } }
    }
    return if (vibrator?.hasVibrator() == true && feel != HapticFeel.Off) play else null
}

internal fun Vibrator.play(feel: HapticFeel) {
    if (feel == HapticFeel.Off || !hasVibrator()) return
    val usage = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    when (feel) {
        HapticFeel.Off -> Unit
        HapticFeel.Tick -> vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK), usage)
        HapticFeel.Click -> vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK), usage)
        HapticFeel.Heavy -> vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK), usage)
        HapticFeel.Double -> vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK), usage)
        HapticFeel.Texture -> vibrate(VibrationEffect.createPredefined(EFFECT_TEXTURE_TICK), usage)
        HapticFeel.Pop -> vibrate(VibrationEffect.createPredefined(EFFECT_POP), usage)
        HapticFeel.Thud -> vibrate(VibrationEffect.createPredefined(EFFECT_THUD), usage)
        HapticFeel.Hold -> vibrate(VibrationEffect.createOneShot(280, VibrationEffect.DEFAULT_AMPLITUDE), usage)
    }
}

private fun Vibrator.holdWave(): VibrationEffect {
    val timings = longArrayOf(0, 600)
    return if (hasAmplitudeControl()) {
        VibrationEffect.createWaveform(timings, intArrayOf(0, 255), 0)
    } else {
        VibrationEffect.createWaveform(timings, 0)
    }
}

internal class HoldHaptic(
    private val vibrator: Vibrator,
    private val feel: () -> HapticFeel,
) {
    fun start() {
        val chosen = feel()
        if (chosen == HapticFeel.Hold) {
            val usage = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
            vibrator.vibrate(vibrator.holdWave(), usage)
        } else {
            vibrator.play(chosen)
        }
    }

    fun stop() {
        vibrator.cancel()
    }
}

@Composable
internal fun rememberHoldHaptic(): HoldHaptic? {
    val vibrator = rememberVibrator()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val feel = rememberUpdatedState(
        if (settings.haptic) settings.hapticProfile.hapticFeel(HapticSlot.Hold) else HapticFeel.Off,
    )
    val hold = remember(vibrator) { vibrator?.let { HoldHaptic(it) { feel.value } } }
    return if (vibrator?.hasVibrator() == true && settings.haptic && feel.value != HapticFeel.Off) hold else null
}

@Composable
private fun rememberVibrator(): Vibrator? {
    val view = LocalView.current
    return remember(view) {
        view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }
}

@Composable
internal fun HapticOnChange(
    key: Any,
    armed: () -> Boolean = { true },
    read: () -> Any?,
) {
    val vibrator = rememberVibrator()
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val feel = if (settings.haptic) settings.hapticProfile.hapticFeel(HapticSlot.Scroll) else HapticFeel.Off
    val current = rememberUpdatedState(feel)
    val gate = rememberUpdatedState(armed)
    LaunchedEffect(key, vibrator) {
        val motor = vibrator?.takeIf { it.hasVibrator() } ?: return@LaunchedEffect
        var previous: Any? = null
        var primed = false
        snapshotFlow(read).collect { value ->
            val chosen = current.value
            if (!primed) {
                previous = value
                primed = true
                return@collect
            }
            // Программный сдвиг (история встала первой, scrollToItem) не щелкает.
            // previous всё равно двигаем, чтобы потом не отстрелять накопившийся скачок.
            if (!gate.value() || value == previous || chosen == HapticFeel.Off) {
                previous = value
                return@collect
            }
            val count = if (value is Int && previous is Int) {
                abs(value - previous as Int).coerceIn(1, 8)
            } else {
                1
            }
            previous = value
            if (chosen == HapticFeel.Tick && count > 1) motor.burst(count) else motor.play(chosen)
        }
    }
}

private fun Vibrator.burst(count: Int) {
    val usage = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    val pulses = count.coerceIn(1, 8)
    val timings = LongArray(pulses * 2)
    val amplitudes = IntArray(pulses * 2)
    for (index in 0 until pulses) {
        timings[index * 2] = if (index == 0) 0L else 16L
        timings[index * 2 + 1] = 12L
        amplitudes[index * 2] = 0
        amplitudes[index * 2 + 1] = 180
    }
    val effect = if (hasAmplitudeControl()) {
        VibrationEffect.createWaveform(timings, amplitudes, -1)
    } else {
        VibrationEffect.createWaveform(timings, -1)
    }
    vibrate(effect, usage)
}

@Composable
internal fun HapticOnAppend(ids: List<String>) {
    val page = rememberHaptic(HapticSlot.Page)
    val current = rememberUpdatedState(ids)
    val play = rememberUpdatedState(page)
    LaunchedEffect(Unit) {
        var previous = emptyList<String>()
        snapshotFlow { current.value }.collect { value ->
            val grew = previous.isNotEmpty() &&
                value.size > previous.size &&
                value.subList(0, previous.size) == previous
            previous = value
            if (grew) play.value?.invoke()
        }
    }
}
