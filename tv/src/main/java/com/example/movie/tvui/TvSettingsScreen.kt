package com.example.movie.tvui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.presentation.settings.LocalUserSettings

@Composable
internal fun TvSettingsScreen(
    menuFocus: FocusRequester,
    posterTarget: TvPosterTarget,
    modifier: Modifier = Modifier,
) {
    val repository = LocalUserSettings.current
    val settings by repository.settings.collectAsStateWithLifecycle()
    val rowFocus = remember { FocusRequester() }
    SideEffect { posterTarget.requester = rowFocus }
    Column(
        modifier
            .fillMaxSize()
            .padding(start = 56.dp, top = 36.dp, end = 48.dp),
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(22.dp))
        TvPlexFrameRow(
            checked = settings.plexBackdrop,
            onChecked = repository::setPlexBackdrop,
            menuFocus = menuFocus,
            rowFocus = rowFocus,
        )
    }
}

@Composable
private fun TvPlexFrameRow(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    menuFocus: FocusRequester,
    rowFocus: FocusRequester,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .focusRequester(rowFocus)
            .onFocusChanged { focused = it.isFocused }
            .focusProperties {
                left = menuFocus
                up = FocusRequester.Cancel
                down = FocusRequester.Cancel
                right = FocusRequester.Cancel
            }
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onChecked(!checked) },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_plex_backdrop),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_plex_backdrop_hint),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        TvSwitch(checked)
    }
}

@Composable
private fun TvSwitch(checked: Boolean) {
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "switch",
    )
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else Color(0xFF2C2C30))
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = travel)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color(0xFF8A8A90)),
        )
    }
}
