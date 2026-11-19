package com.example.movie.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.example.movie.R

internal fun NotificationCompat.Builder.appIcon(context: Context): NotificationCompat.Builder {
    setSmallIcon(R.drawable.ic_stat_lumen)
    AppNotificationIcon.large(context)?.let { setLargeIcon(it) }
    return this
}

private object AppNotificationIcon {
    private var large: Bitmap? = null

    fun large(context: Context): Bitmap? {
        large?.let { return it }
        val options = BitmapFactory.Options().apply { inScaled = false }
        val raw = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_lumen_metal, options)
            ?: return null
        val scaled = Bitmap.createScaledBitmap(raw, 192, 192, true)
        if (scaled !== raw) raw.recycle()
        large = scaled
        return scaled
    }
}
