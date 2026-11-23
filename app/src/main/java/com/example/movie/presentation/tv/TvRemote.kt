package com.example.movie.presentation.tv

import android.content.res.Configuration
import android.graphics.Rect
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
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
        .onKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
            val direction = event.key.toFocusDirection() ?: return@onKeyEvent false
            runCatching { view.moveTelevisionFocus(direction) }.getOrDefault(false)
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

private fun Key.toFocusDirection(): FocusDirection? = when (this) {
    Key.DirectionLeft -> FocusDirection.Left
    Key.DirectionRight -> FocusDirection.Right
    Key.DirectionUp -> FocusDirection.Up
    Key.DirectionDown -> FocusDirection.Down
    else -> null
}

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
        val next = host.focusedComposeRect(screen)
        if (!next) {
            if (shown) {
                shown = false
                invalidate()
            }
            return
        }
        getLocationInWindow(location)
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

private fun View.focusedComposeRect(out: Rect): Boolean {
    val bounds = focusedLeaf()?.second ?: return false
    out.set(
        bounds.left.toInt(),
        bounds.top.toInt(),
        bounds.right.toInt(),
        bounds.bottom.toInt(),
    )
    return true
}

private fun View.moveTelevisionFocus(direction: FocusDirection): Boolean {
    val leaves = focusLeaves()
    val source = leaves.firstOrNull { (node, _) -> node.isFocusedNode() }?.second ?: return false
    var bestNode: SemanticsNode? = null
    var bestBounds = ComposeRect.Zero
    for ((node, bounds) in leaves) {
        if (node.isFocusedNode() || !bounds.isFocusCandidate(source, direction)) continue
        if (bestNode == null || bounds.beats(bestBounds, source, direction)) {
            bestNode = node
            bestBounds = bounds
        }
    }
    val winner = bestNode
    if (winner == null) {
        return if (direction == FocusDirection.Left || direction == FocusDirection.Right) {
            scrollContent(source, direction)
        } else {
            false
        }
    }
    val axis = if (direction == FocusDirection.Left || direction == FocusDirection.Right) width else height
    val intoChrome = direction == FocusDirection.Down && bestBounds.top > height * 0.8f
    if (intoChrome || bestBounds.majorDistance(source, direction) > axis * 0.45f) {
        if (scrollContent(source, direction)) return true
    }
    if (bestBounds.majorDistance(source, direction) > axis * 0.62f) return false
    return winner.config.getOrNull(SemanticsActions.RequestFocus)?.action?.invoke() == true
}

private fun View.scrollContent(source: ComposeRect, direction: FocusDirection): Boolean {
    val vertical = direction == FocusDirection.Up || direction == FocusDirection.Down
    val node = scrollContainer(source, vertical) ?: return false
    val range = node.config.getOrNull(
        if (vertical) SemanticsProperties.VerticalScrollAxisRange
        else SemanticsProperties.HorizontalScrollAxisRange,
    ) ?: return false
    val forward = if (range.reverseScrolling) {
        direction == FocusDirection.Up || direction == FocusDirection.Left
    } else {
        direction == FocusDirection.Down || direction == FocusDirection.Right
    }
    val value = range.value()
    val max = range.maxValue()
    val can = if (forward) value < max - 1f else value > 1f
    if (!can) return false
    val action = node.config.getOrNull(SemanticsActions.ScrollBy)?.action ?: return false
    val step = 220f * resources.displayMetrics.density
    val delta = if (forward) step else -step
    return if (vertical) action(0f, delta) else action(delta, 0f)
}

private fun View.scrollContainer(source: ComposeRect, vertical: Boolean): SemanticsNode? {
    val owner = this as? RootForTest ?: return null
    var best: SemanticsNode? = null
    var bestArea = Float.MAX_VALUE
    fun walk(node: SemanticsNode) {
        val range = node.config.getOrNull(
            if (vertical) SemanticsProperties.VerticalScrollAxisRange
            else SemanticsProperties.HorizontalScrollAxisRange,
        )
        val bounds = node.boundsInWindow
        val area = bounds.width * bounds.height
        if (range != null && bounds.contains(source.center) && area < bestArea) {
            best = node
            bestArea = area
        }
        node.children.forEach(::walk)
    }
    walk(owner.semanticsOwner.unmergedRootSemanticsNode)
    return best
}

private fun View.focusLeaves(): List<Pair<SemanticsNode, ComposeRect>> {
    val owner = this as? RootForTest ?: return emptyList()
    val all = ArrayList<SemanticsNode>(96)
    fun collect(node: SemanticsNode) {
        all.add(node)
        node.children.forEach(::collect)
    }
    collect(owner.semanticsOwner.unmergedRootSemanticsNode)
    val targets = ArrayList<Pair<SemanticsNode, ComposeRect>>(24)
    for (node in all) {
        if (node.config.getOrNull(SemanticsActions.RequestFocus)?.action == null) continue
        val bounds = node.boundsInWindow
        if (bounds.width < 8f || bounds.height < 8f) continue
        if (bounds.width > width * 0.92f && bounds.height > height * 0.92f) continue
        targets.add(node to bounds)
    }
    val ids = targets.map { it.first.id }.toHashSet()
    fun SemanticsNode.holdsFocusableChild(): Boolean {
        for (child in children) {
            if (child.id in ids || child.holdsFocusableChild()) return true
        }
        return false
    }
    return targets.filter { (node, _) -> !node.holdsFocusableChild() }
}

private fun View.focusedLeaf(): Pair<SemanticsNode, ComposeRect>? =
    focusLeaves().firstOrNull { (node, _) -> node.isFocusedNode() }

private fun SemanticsNode.isFocusedNode(): Boolean =
    config.getOrNull(SemanticsProperties.Focused) == true

private fun ComposeRect.isFocusCandidate(source: ComposeRect, direction: FocusDirection): Boolean =
    when (direction) {
        FocusDirection.Left ->
            (source.right > right || source.left >= right) && source.left > left
        FocusDirection.Right ->
            (source.left < left || source.right <= left) && source.right < right
        FocusDirection.Up ->
            (source.bottom > bottom || source.top >= bottom) && source.top > top
        FocusDirection.Down ->
            (source.top < top || source.bottom <= top) && source.bottom < bottom
        else -> false
    }

private fun ComposeRect.majorDistance(source: ComposeRect, direction: FocusDirection): Float =
    when (direction) {
        FocusDirection.Left -> (source.left - right).coerceAtLeast(0f)
        FocusDirection.Right -> (left - source.right).coerceAtLeast(0f)
        FocusDirection.Up -> (source.top - bottom).coerceAtLeast(0f)
        FocusDirection.Down -> (top - source.bottom).coerceAtLeast(0f)
        else -> 0f
    }

private fun ComposeRect.beats(
    other: ComposeRect,
    source: ComposeRect,
    direction: FocusDirection,
): Boolean {
    fun ComposeRect.inBeam() = when (direction) {
        FocusDirection.Left, FocusDirection.Right -> bottom > source.top && top < source.bottom
        FocusDirection.Up, FocusDirection.Down -> right > source.left && left < source.right
        else -> false
    }
    fun ComposeRect.inDirection() = when (direction) {
        FocusDirection.Left -> source.left >= right
        FocusDirection.Right -> source.right <= left
        FocusDirection.Up -> source.top >= bottom
        FocusDirection.Down -> source.bottom <= top
        else -> false
    }
    fun ComposeRect.farEdge() = when (direction) {
        FocusDirection.Left -> (source.left - left).coerceAtLeast(1f)
        FocusDirection.Right -> (right - source.right).coerceAtLeast(1f)
        FocusDirection.Up -> (source.top - top).coerceAtLeast(1f)
        FocusDirection.Down -> (bottom - source.bottom).coerceAtLeast(1f)
        else -> 1f
    }
    val beamWins = when {
        other.inBeam() || !inBeam() -> false
        !other.inDirection() -> true
        direction == FocusDirection.Left || direction == FocusDirection.Right -> true
        else -> majorDistance(source, direction) < other.farEdge()
    }
    if (beamWins) return true
    val otherWins = when {
        inBeam() || !other.inBeam() -> false
        !inDirection() -> true
        direction == FocusDirection.Left || direction == FocusDirection.Right -> true
        else -> other.majorDistance(source, direction) < farEdge()
    }
    if (otherWins) return false
    return weighted(source, direction) < other.weighted(source, direction)
}

private fun ComposeRect.weighted(source: ComposeRect, direction: FocusDirection): Long {
    val major = majorDistance(source, direction).toLong()
    val minor = when (direction) {
        FocusDirection.Left, FocusDirection.Right ->
            (source.top + source.height / 2f) - (top + height / 2f)
        else -> (source.left + source.width / 2f) - (left + width / 2f)
    }.toLong()
    return 13 * major * major + minor * minor
}
