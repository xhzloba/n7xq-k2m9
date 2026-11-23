package com.example.movie.presentation.tv

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration

val LocalTelevision = staticCompositionLocalOf { false }

@Composable
fun TelevisionProvider(content: @Composable () -> Unit) {
    val mode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK
    val television = mode == Configuration.UI_MODE_TYPE_TELEVISION
    CompositionLocalProvider(
        LocalTelevision provides television,
        content = content,
    )
}

fun Modifier.dpadSkip(): Modifier = this

fun Modifier.tvWall(active: Boolean): Modifier = this

fun Modifier.remoteEntry(): Modifier = this

fun Modifier.tvRemoteRoot(): Modifier = this
