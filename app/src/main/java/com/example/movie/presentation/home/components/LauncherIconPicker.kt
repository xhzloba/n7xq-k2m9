package com.example.movie.presentation.home.components

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R

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

    fun apply(context: Context, id: String) {
        val pm = context.packageManager
        val pkg = context.packageName
        LauncherMark.entries.forEach { mark ->
            pm.setComponentEnabledSetting(
                ComponentName(pkg, pkg + mark.alias),
                if (mark.id == id) {
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
    }
}

@Composable
internal fun LauncherIconPicker(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = stringResource(R.string.launcher_icon),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LauncherMark.entries.forEach { mark ->
                    Mark(mark, mark.id == selected, shape, onSelect)
                }
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
