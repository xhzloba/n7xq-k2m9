package com.example.movie.presentation.common

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal const val NoticeShelfFavorites = "Favorites"
internal const val NoticeShelfWatched = "Watched"
internal const val NoticeShelfDownloads = "Downloads"

private const val NoticeDelayMillis = 1_500L
private const val NoticeHoldMillis = 2_000L

internal class ShelfNotices(
    private val scope: CoroutineScope?,
    private val context: Context?,
) {
    var openRequest by mutableIntStateOf(0)
    var section by mutableStateOf(NoticeShelfFavorites)
    var jump: () -> Unit = {}

    private var job: Job? = null
    private var toast: Toast? = null
    private var token = 0

    fun show(
        message: String,
        shelf: String,
        delayMillis: Long = NoticeDelayMillis,
        holdMillis: Long = NoticeHoldMillis,
    ) {
        val scope = scope ?: return
        val context = context ?: return
        section = shelf
        val ticket = ++token
        job?.cancel()
        toast?.cancel()
        job = scope.launch {
            delay(delayMillis)
            if (ticket != token) return@launch
            val length = if (holdMillis > NoticeHoldMillis) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            toast = Toast.makeText(context.applicationContext, message, length).also { it.show() }
        }
    }

    fun dismiss() {
        token++
        job?.cancel()
        toast?.cancel()
        toast = null
    }

    fun open() {
        token++
        job?.cancel()
        toast?.cancel()
        toast = null
        openRequest++
        jump()
    }
}

internal val LocalShelfNotices = staticCompositionLocalOf { ShelfNotices(null, null) }
