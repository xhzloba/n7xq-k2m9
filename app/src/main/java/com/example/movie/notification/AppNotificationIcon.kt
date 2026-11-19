package com.example.movie.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.movie.R

internal fun NotificationCompat.Builder.appIcon(context: Context): NotificationCompat.Builder {
    setSmallIcon(R.drawable.ic_stat_lumen)
    setColor(context.getColor(R.color.ic_launcher_plate))
    return this
}
