package com.example.movie.presentation.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.movie.R
import com.example.movie.data.backup.AppBackup
import com.example.movie.di.LocalAppContainer
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.CoverPopup
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DataPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val theme by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val panel = if (theme.appTheme == THEME_GRAPHITE) Color(0xFF22262E) else CinemaSurface
    val tick = rememberHapticTick()
    val scope = rememberCoroutineScope()
    val backup = remember(container) {
        AppBackup(
            context = context.applicationContext,
            settings = container.userSettings,
            favorites = container.favorites,
            watched = container.watched,
            history = container.viewHistory,
            resume = container.resume,
            search = container.searchHistory,
            alerts = container.releaseWatch.store,
            backdrops = container.backdropSets,
        )
    }
    var confirm by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val saved = stringResource(R.string.settings_data_exported)
    val restored = stringResource(R.string.settings_data_imported)
    val rejected = stringResource(R.string.settings_data_import_failed)
    val cacheDone = stringResource(R.string.settings_data_cache_done)
    val wipeDone = stringResource(R.string.settings_data_wipe_done)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { backup.export() }
            val wrote = withContext(Dispatchers.IO) { writeText(context, uri, text) }
            notice = if (wrote) saved else rejected
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) { readText(context, uri) }
            val ok = text != null && withContext(Dispatchers.IO) { backup.import(text) }
            notice = if (ok) restored else rejected
        }
    }
    Column(modifier) {
        Text(
            text = stringResource(R.string.settings_data_body),
            modifier = Modifier.padding(start = PosterGridInset + 4.dp, end = PosterGridInset, bottom = 16.dp),
            color = CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        DataAction(
            panel = panel,
            title = stringResource(R.string.settings_data_export),
            hint = stringResource(R.string.settings_data_export_hint),
            onClick = {
                tick?.invoke()
                export.launch("AmigoPlay.json")
            },
        )
        Spacer(Modifier.height(10.dp))
        DataAction(
            panel = panel,
            title = stringResource(R.string.settings_data_import),
            hint = stringResource(R.string.settings_data_import_hint),
            onClick = {
                tick?.invoke()
                import.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
        )
        Spacer(Modifier.height(22.dp))
        DataAction(
            panel = panel,
            title = stringResource(R.string.settings_data_cache),
            hint = stringResource(R.string.settings_data_cache_hint),
            onClick = {
                tick?.invoke()
                confirm = "cache"
            },
        )
        Spacer(Modifier.height(10.dp))
        DataAction(
            panel = panel,
            title = stringResource(R.string.settings_data_wipe),
            hint = stringResource(R.string.settings_data_wipe_hint),
            onClick = {
                tick?.invoke()
                confirm = "data"
            },
        )
        if (notice != null) {
            Text(
                text = notice.orEmpty(),
                modifier = Modifier.padding(start = PosterGridInset + 4.dp, end = PosterGridInset, top = 16.dp),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            )
        }
    }
    val pending = confirm
    if (pending != null) {
        val cache = pending == "cache"
        CoverPopup(onDismiss = { confirm = null }) {
            DataConfirm(
                title = stringResource(if (cache) R.string.settings_data_cache_title else R.string.settings_data_wipe_title),
                body = stringResource(if (cache) R.string.settings_data_cache_body else R.string.settings_data_wipe_body),
                onConfirm = {
                    confirm = null
                    if (cache) {
                        scope.launch {
                            withContext(Dispatchers.IO) { backup.clearCache() }
                            notice = cacheDone
                        }
                    } else {
                        backup.clearData()
                        notice = wipeDone
                    }
                },
                onCancel = { confirm = null },
            )
        }
    }
}

@Composable
private fun DataConfirm(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val tick = rememberHapticTick()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 23.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 15.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.settings_data_yes),
            color = Color.White,
            fontSize = 16.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onConfirm()
                    },
                )
                .padding(vertical = 12.dp),
        )
        Text(
            text = stringResource(R.string.settings_data_no),
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 16.sp,
            lineHeight = 21.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        onCancel()
                    },
                )
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
private fun DataAction(panel: Color, title: String, hint: String, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = PosterGridInset)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(panel)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = hint,
            modifier = Modifier.padding(top = 4.dp),
            color = CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

private fun writeText(context: android.content.Context, uri: Uri, text: String): Boolean =
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } != null
    }.getOrDefault(false)

private fun readText(context: android.content.Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    }.getOrNull()
