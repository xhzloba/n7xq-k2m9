package com.example.movie.presentation.detail

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Наклон относительно позы, в которой открыли карточку. −1…1 по обеим осям. */
@Composable
internal fun rememberDetailTilt(enabled: Boolean, resetKey: Any): State<Offset> {
    val tilt = remember(resetKey) { mutableStateOf(Offset.Zero) }
    val context = LocalContext.current
    DisposableEffect(enabled, resetKey, context) {
        tilt.value = Offset.Zero
        if (!enabled) return@DisposableEffect onDispose {}
        val manager = context.getSystemService(SensorManager::class.java)
        val rotation = manager?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val gravity = if (rotation == null) manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) else null
        val sensor = rotation ?: gravity
        if (manager == null || sensor == null) return@DisposableEffect onDispose {}
        val main = Handler(Looper.getMainLooper())
        val listener = TiltListener(rotation != null) { next ->
            main.post { tilt.value = next }
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager.unregisterListener(listener) }
    }
    return tilt
}

/**
 * Кадр больше окна, родитель его клипует. Наклон сдвигает картинку внутри рамки
 * и чуть поворачивает плоскость — виден другой кусок кадра, не съехавший прямоугольник.
 */
internal fun Modifier.backdropPan(tilt: State<Offset>, shift: Float, fade: Float): Modifier {
    if (fade <= 0.001f) return this
    val bleed = 0.48f * fade
    return layout { measurable, constraints ->
        val bw = constraints.maxWidth
        val bh = constraints.maxHeight
        if (bw <= 0 || bh <= 0) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        } else {
            val w = (bw * (1f + bleed)).roundToInt()
            val h = (bh * (1f + bleed)).roundToInt()
            val placeable = measurable.measure(Constraints.fixed(w, h))
            layout(bw, bh) {
                val sample = tilt.value
                val finite = sample.x.isFinite() && sample.y.isFinite()
                val tx = if (finite) -sample.x * shift * fade else 0f
                val ty = if (finite) -sample.y * shift * 0.62f * fade else 0f
                placeable.place(
                    ((bw - w) / 2f + tx).roundToInt(),
                    ((bh - h) / 2f + ty).roundToInt(),
                )
            }
        }
    }.graphicsLayer {
        val sample = tilt.value
        if (!sample.x.isFinite() || !sample.y.isFinite()) return@graphicsLayer
        rotationY = sample.x * 6f * fade
        rotationX = -sample.y * 4.5f * fade
    }
}

/** Сдвиг без graphicsLayer: слой на verticalScroll прятал всю карточку. */
internal fun Modifier.contentParallax(tilt: State<Offset>, shift: Float, fade: Float): Modifier = offset {
    val sample = tilt.value
    if (fade <= 0f || !sample.x.isFinite() || !sample.y.isFinite()) {
        IntOffset.Zero
    } else {
        IntOffset(
            (sample.x * shift * fade).roundToInt(),
            (sample.y * shift * fade).roundToInt(),
        )
    }
}

private class TiltListener(
    private val rotationVector: Boolean,
    private val onTilt: (Offset) -> Unit,
) : SensorEventListener {
    private var baseX = Float.NaN
    private var baseY = Float.NaN
    private var smoothX = 0f
    private var smoothY = 0f

    override fun onSensorChanged(event: SensorEvent) {
        val sample = if (rotationVector) fromRotation(event.values) else fromGravity(event.values)
        if (!sample.x.isFinite() || !sample.y.isFinite()) return
        if (!baseX.isFinite() || !baseY.isFinite()) {
            baseX = sample.x
            baseY = sample.y
            return
        }
        val dx = ((baseX - sample.x) / Swing).coerceIn(-1f, 1f)
        val dy = ((baseY - sample.y) / Swing).coerceIn(-1f, 1f)
        smoothX += (dx - smoothX) * 0.35f
        smoothY += (dy - smoothY) * 0.35f
        if (!smoothX.isFinite() || !smoothY.isFinite()) {
            smoothX = 0f
            smoothY = 0f
            return
        }
        onTilt(Offset(smoothX, smoothY))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun fromRotation(values: FloatArray): Offset {
        if (values.size < 3) return Offset(Float.NaN, Float.NaN)
        val vector = FloatArray(4)
        vector[0] = values[0]
        vector[1] = values[1]
        vector[2] = values[2]
        if (values.size >= 4) {
            vector[3] = values[3]
        } else {
            val rest = 1f - vector[0] * vector[0] - vector[1] * vector[1] - vector[2] * vector[2]
            vector[3] = if (rest > 0f) sqrt(rest) else 0f
        }
        val rotation = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotation, vector)
        // Куда смотрит небо в осях экрана: в портрете это (0, 1, 0), без скачка ±180°.
        return Offset(rotation[6], rotation[8])
    }

    private fun fromGravity(values: FloatArray): Offset {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val norm = sqrt(x * x + y * y + z * z).coerceAtLeast(0.1f)
        return Offset(x / norm, z / norm)
    }

    private companion object {
        const val Swing = 0.34f
    }
}
