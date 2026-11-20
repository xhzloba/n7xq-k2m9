package com.example.movie.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.movie.MainActivity
import com.example.movie.R
import com.example.movie.notification.appIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class MovieDownloadService : DownloadService(
    NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    R.string.download_channel_description,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val posters = LinkedHashMap<String, Bitmap>()
    private val posterLoads = HashSet<String>()
    override fun onCreate() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.download_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                    description = getString(R.string.download_channel_description)
                },
            )
        }
        super.onCreate()
    }

    override fun getDownloadManager(): DownloadManager = MediaCache.manager(this)

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification {
        val active = downloads.firstOrNull { it.state != Download.STATE_REMOVING }
        val payload = active?.request?.data
        val title = payload?.downloadTitle()?.takeIf { it.isNotBlank() }
            ?: getString(R.string.download_channel_name)
        val percent = active?.percentDownloaded?.takeIf { it >= 0f }?.toInt()
        val text = when {
            notMetRequirements != 0 -> getString(R.string.download_waiting)
            percent != null -> getString(R.string.download_progress, percent)
            else -> getString(R.string.download_running)
        }
        val movieId = active?.request?.id?.substringBefore('#')?.takeIf { it.isNotBlank() }
        val open = movieId?.let { id ->
            PendingIntent.getActivity(
                this,
                NOTIFICATION_ID,
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_MOVIE, id)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val art = poster(payload?.downloadPoster())
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .appIcon(this)
            .apply { if (art != null) setLargeIcon(art) }
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, percent ?: 0, percent == null)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun poster(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        synchronized(posters) { posters[url] }?.let { return it }
        synchronized(posterLoads) {
            if (!posterLoads.add(url)) return null
        }
        scope.launch {
            val bitmap = runCatching {
                val result = imageLoader.execute(
                    ImageRequest.Builder(this@MovieDownloadService)
                        .data(url)
                        .size(256)
                        .allowHardware(false)
                        .build(),
                )
                (result as? SuccessResult)?.drawable?.toBitmap()
            }.getOrNull()
            synchronized(posterLoads) { posterLoads.remove(url) }
            if (bitmap == null) return@launch
            synchronized(posters) {
                if (posters.size >= 6) posters.remove(posters.keys.first())
                posters[url] = bitmap
            }
            invalidateForegroundNotification()
        }
        return null
    }

    private companion object {
        const val CHANNEL_ID = "offline"
        const val NOTIFICATION_ID = 41
    }
}
