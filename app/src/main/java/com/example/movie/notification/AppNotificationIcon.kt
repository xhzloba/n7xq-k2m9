package com.example.movie.notification

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import com.example.movie.R

internal fun NotificationCompat.Builder.appIcon(context: Context): NotificationCompat.Builder {
    setSmallIcon(R.drawable.ic_stat_notify)
    setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_lumen_metal))
    setColor(android.graphics.Color.WHITE)
    return this
}
