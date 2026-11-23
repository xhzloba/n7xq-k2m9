package com.example.movie.presentation.home.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.ui.theme.CinemaMuted

internal enum class LauncherMark(
    val id: String,
    val icon: Int,
    val alias: String,
) {
    Brushed("brushed", R.mipmap.ic_brand_brushed_fg, ".LauncherBrushed"),
    White("white", R.mipmap.ic_brand_white_fg, ".LauncherWhite"),
}

internal object LauncherIcons {
    private val retired = listOf(
        ".LauncherNeon",
        ".LauncherMetal",
        ".LauncherMatte",
        ".LauncherNavy",
    )

    fun settle(context: Context) {
        val pm = context.packageManager
        val pkg = context.packageName
        fun enabled(alias: String) = pm.getComponentEnabledSetting(ComponentName(pkg, pkg + alias)) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        val live = LauncherMark.entries.filter { enabled(it.alias) }
        if (retired.any { enabled(it) } || live.size != 1) {
            apply(context, live.firstOrNull()?.id ?: LauncherMark.Brushed.id)
        }
    }

    fun current(context: Context): String {
        val pm = context.packageManager
        val pkg = context.packageName
        val on = LauncherMark.entries.firstOrNull { mark ->
            val state = pm.getComponentEnabledSetting(ComponentName(pkg, pkg + mark.alias))
            when (state) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
                else -> mark == LauncherMark.Brushed
            }
        }
        return on?.id ?: LauncherMark.Brushed.id
    }

    fun apply(context: Context, id: String, restart: Boolean = false) {
        val pm = context.packageManager
        val pkg = context.packageName
        val chosen = LauncherMark.entries.firstOrNull { it.id == id } ?: LauncherMark.Brushed
        LauncherMark.entries.forEach { mark ->
            pm.setComponentEnabledSetting(
                ComponentName(pkg, pkg + mark.alias),
                if (mark.id == chosen.id) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                },
                PackageManager.DONT_KILL_APP,
            )
        }
        retired.forEach { alias ->
            pm.setComponentEnabledSetting(
                ComponentName(pkg, pkg + alias),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
        if (!restart) return
        val launch = Intent.makeRestartActivityTask(ComponentName(pkg, pkg + chosen.alias))
        context.startActivity(launch)
        Handler(Looper.getMainLooper()).post {
            Process.killProcess(Process.myPid())
        }
    }
}

@Composable
internal fun LauncherIconChoice(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(LauncherIcons.current(context)) }
    val tick = rememberHapticTick()
    val shape = RoundedCornerShape(18.dp)
    Column(modifier) {
        Text(
            text = stringResource(R.string.launcher_icon),
            color = CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LauncherMark.entries.forEach { mark ->
                Mark(
                    mark = mark,
                    selected = mark.id == selected,
                    shape = shape,
                    onSelect = { id ->
                        if (id != selected) {
                            tick?.invoke()
                            selected = id
                            LauncherIcons.apply(context, id, restart = true)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Mark(
    mark: LauncherMark,
    selected: Boolean,
    shape: RoundedCornerShape,
    onSelect: (String) -> Unit,
) {
    Image(
        painter = painterResource(mark.icon),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(72.dp)
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.28f),
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onSelect(mark.id) },
            ),
    )
}
