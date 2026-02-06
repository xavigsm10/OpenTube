package com.opentube

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe
import com.opentube.data.extractor.NewPipeDownloaderImpl

import coil.ImageLoader
import coil.ImageLoaderFactory

@HiltAndroidApp
class OpenTubeApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        
        // Inicializar NewPipe Extractor con el downloader correcto
        NewPipe.init(NewPipeDownloaderImpl())
        
        // Crear canal de notificaciones para reproducción de media
        createNotificationChannel()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .allowHardware(false) // CRITICAL: Fix for black screen/freezing on emulators
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "playback_channel",
                "Reproducción de Media",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción de video"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
