package com.example.movie

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.example.movie.data.download.MediaCache
import com.example.movie.data.download.resumePendingDownloads
import com.example.movie.di.AppContainer
import com.example.movie.presentation.alerts.ReleaseWatchReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Dispatcher
import okhttp3.OkHttpClient

class MovieApplication : Application() {
    lateinit var container: AppContainer
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        MediaCache.install(this)
        container = AppContainer(this)
        resumePendingDownloads(this)
        ReleaseWatchReceiver.schedule(this)
        val imageDispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 16
        }
        val imageClient = OkHttpClient.Builder()
            .dispatcher(imageDispatcher)
            .build()
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(imageClient)
                .crossfade(350)
                .build(),
        )
    }
}
