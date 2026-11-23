package com.example.movie.presentation.tv

import android.content.res.Configuration
import android.graphics.Rect
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import android.widget.FrameLayout
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

val LocalTelevision = staticCompositionLocalOf { false }

private class RemoteEntries {
    var current by mutableStateOf<FocusRequester?>(null)
}

private val LocalRemoteEntry = staticCompositionLocalOf<RemoteEntries?> { null }

@Composable
fun TelevisionProvider(content: @Composable () -> Unit) {
    val mode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    val television = mode == Configuration.UI_MODE_TYPE_TELEVISION
    val entries = remember { RemoteEntries() }
    CompositionLocalProvider(
        LocalTelevision provides television,
        LocalRemoteEntry provides entries,
        content = content,
    )
}

/** Пустой перехват тапа: на пульте не становится целью. */
fun Modifier.dpadSkip(): Modifier = focusProperties { canFocus = false }

/** Прячет дочерние цели, пока сверху лежит другой слой. На телефоне no-op. */
fun Modifier.tvWall(active: Boolean): Modifier = composed {
    if (active && LocalTelevision.current) this.focusable() else this
}

/** Текущий слой, в который пульт входит первым. На телефоне no-op. */
fun Modifier.remoteEntry(): Modifier = composed {
    val entry = LocalRemoteEntry.current
    if (!LocalTelevision.current || entry == null) return@composed this
    val requester = remember { FocusRequester() }
    DisposableEffect(requester) {
        val previous = entry.current
        entry.current = requester
        onDispose {
            if (entry.current === requester) entry.current = previous
        }
    }
    this.focusRequester(requester)
}

/** Кольцо фокуса и первый вход с D-pad. На телефоне no-op. */
fun Modifier.tvRemoteRoot(): Modifier = composed {
    val entry = LocalRemoteEntry.current
    if (!LocalTelevision.current || entry == null) return@composed this
    val view = LocalView.current
    DisposableEffect(view) {
        val ring = FocusRingView(view.context)
        val decor = view.rootView as ViewGroup
        decor.addView(
            ring,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        val choreographer = Choreographer.getInstance()
        var frame = 0
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (!view.isAttachedToWindow) return
                if (frame++ % 2 == 0) runCatching { ring.sync(view) }
                choreographer.postFrameCallback(this)
            }
        }
        choreographer.postFrameCallback(callback)
        onDispose {
            choreographer.removeFrameCallback(callback)
            decor.removeView(ring)
        }
    }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(entry.current) {
        val target = entry.current ?: return@LaunchedEffect
        repeat(8) {
            if (runCatching { target.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
            delay(150)
        }
    }
    this
        .onFocusChanged { focused = it.hasFocus }
        .onPreviewKeyEvent { event ->
            if (focused || event.type != KeyEventType.KeyDown || !event.key.isDpad) return@onPreviewKeyEvent false
            val target = entry.current ?: return@onPreviewKeyEvent false
            runCatching { target.requestFocus() }.getOrDefault(false)
        }
}

private val Key.isDpad: Boolean
    get() = this == Key.DirectionUp ||
        this == Key.DirectionDown ||
        this == Key.DirectionLeft ||
        this == Key.DirectionRight ||
        this == Key.DirectionCenter ||
        this == Key.Enter ||
        this == Key.NumPadEnter

private class FocusRingView(context: android.content.Context) : View(context) {
    private val hole = Rect()
    private val screen = Rect()
    private val location = IntArray(2)
    private var shown = false
    private val stroke = 3f * resources.displayMetrics.density
    private val radius = 14f * resources.displayMetrics.density

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    fun sync(host: View) {
        val provider = host.accessibilityNodeProvider
        val next = provider?.focusedRect(screen) ?: false
        if (!next) {
            if (shown) {
                shown = false
                invalidate()
            }
            return
        }
        getLocationOnScreen(location)
        val left = screen.left - location[0]
        val top = screen.top - location[1]
        val right = screen.right - location[0]
        val bottom = screen.bottom - location[1]
        val wide = right - left
        val tall = bottom - top
        val giant = wide > width * 0.92f && tall > height * 0.92f
        if (wide < 8 || tall < 8 || giant) {
            if (shown) {
                shown = false
                invalidate()
            }
            return
        }
        if (hole.left == left && hole.top == top && hole.right == right && hole.bottom == bottom && shown) return
        hole.set(left, top, right, bottom)
        shown = true
        invalidate()
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        if (!shown) return
        val paint = paint
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = stroke + 2f * resources.displayMetrics.density
        paint.color = android.graphics.Color.BLACK
        canvas.drawRoundRect(
            hole.left.toFloat(),
            hole.top.toFloat(),
            hole.right.toFloat(),
            hole.bottom.toFloat(),
            radius,
            radius,
            paint,
        )
        paint.strokeWidth = stroke
        paint.color = android.graphics.Color.WHITE
        canvas.drawRoundRect(
            hole.left.toFloat(),
            hole.top.toFloat(),
            hole.right.toFloat(),
            hole.bottom.toFloat(),
            radius,
            radius,
            paint,
        )
    }

    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
}

private fun AccessibilityNodeProvider.focusedRect(out: Rect): Boolean {
    val root = createAccessibilityNodeInfo(AccessibilityNodeProvider.HOST_VIEW_ID) ?: return false
    root.seal()
    val focused = root.findFocusedNode()
    if (focused !== root) root.recycle()
    if (focused == null) return false
    focused.seal()
    focused.getBoundsInScreen(out)
    focused.recycle()
    return true
}

private fun AccessibilityNodeInfo.seal() {
    if (!isSealed) setSealed(true)
}

private fun AccessibilityNodeInfo.findFocusedNode(): AccessibilityNodeInfo? {
    seal()
    if (isFocused) return this
    for (index in 0 until childCount) {
        val child = getChild(index) ?: continue
        val hit = child.findFocusedNode()
        if (hit != null) {
            if (hit !== child) child.recycle()
            return hit
        }
        child.recycle()
    }
    return null
}
