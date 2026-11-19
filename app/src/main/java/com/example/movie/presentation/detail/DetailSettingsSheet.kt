package com.example.movie.presentation.detail

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.movie.R
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.settings.LocalUserSettings
import com.example.movie.ui.theme.CinemaMuted

@Composable
internal fun DetailSettingsPage() {
    val store = LocalUserSettings.current
    val settings by store.settings.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.detail_settings),
            modifier = Modifier.padding(horizontal = 22.dp),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
        ) {
            DetailToggle(
                title = stringResource(R.string.settings_logos),
                hint = stringResource(R.string.settings_logos_hint),
                checked = settings.showLogos,
                onChecked = store::setShowLogos,
            )
            DetailToggle(
                title = stringResource(R.string.settings_backdrop_auto),
                hint = stringResource(R.string.settings_backdrop_auto_hint),
                checked = settings.autoBackdrop,
                onChecked = store::setAutoBackdrop,
            )
            DetailToggle(
                title = stringResource(R.string.settings_backdrop_dissolve),
                hint = stringResource(R.string.settings_backdrop_dissolve_hint),
                checked = settings.backdropDissolve,
                onChecked = store::setBackdropDissolve,
            )
            DetailToggle(
                title = stringResource(R.string.settings_watch_glow),
                hint = stringResource(R.string.settings_watch_glow_hint),
                checked = settings.watchGlow,
                onChecked = store::setWatchGlow,
            )
            DetailToggle(
                title = stringResource(R.string.settings_detail_iris),
                hint = stringResource(R.string.settings_detail_iris_hint),
                checked = settings.detailIris,
                onChecked = store::setDetailIris,
            )
            DetailToggle(
                title = stringResource(R.string.settings_extract_backdrop),
                hint = stringResource(R.string.settings_extract_backdrop_hint),
                checked = settings.extractBackdrop,
                onChecked = store::setExtractBackdrop,
            )
            DetailToggle(
                title = stringResource(R.string.settings_extract_plex),
                hint = stringResource(R.string.settings_extract_plex_hint),
                checked = settings.extractPlex,
                onChecked = store::setExtractPlex,
            )
            if (settings.extractPlex) {
                DetailToggle(
                    title = stringResource(R.string.settings_plex_backdrop),
                    hint = stringResource(R.string.settings_plex_backdrop_hint),
                    checked = settings.plexBackdrop,
                    onChecked = store::setPlexBackdrop,
                )
            }
            DetailToggle(
                title = stringResource(R.string.settings_gyro),
                hint = stringResource(R.string.settings_gyro_hint),
                checked = settings.gyro,
                onChecked = store::setGyro,
            )
            if (settings.gyro) {
                DetailToggle(
                    title = stringResource(R.string.settings_parallax),
                    hint = stringResource(R.string.settings_parallax_hint),
                    checked = settings.detailParallax,
                    onChecked = store::setDetailParallax,
                )
            }
        }
    }
}

@Composable
private fun DetailToggle(
    title: String,
    hint: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    val tick = rememberHapticTick()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    tick?.invoke()
                    onChecked(!checked)
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 14.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = hint,
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
            )
        }
        DetailSwitch(checked)
    }
}

@Composable
private fun DetailSwitch(checked: Boolean) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val off = if (settings.appTheme == THEME_GRAPHITE) Color(0xFF2A2F38) else Color(0xFF2C2C30)
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "detailSwitch",
    )
    Box(
        Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else off)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = travel)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color(0xFF8A8A90)),
        )
    }
}
