package com.tsourcecode.wiki.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tsourcecode.wiki.app.AppComponent
import com.tsourcecode.wiki.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SyncForegroundService : Service() {
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var collectJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopCollecting()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                ensureChannel()
                val defaultTitle = getString(R.string.sync_notification_channel)
                startForeground(NOTIFICATION_ID, buildNotification(defaultTitle))
                startCollecting()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopCollecting()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startCollecting() {
        collectJob?.cancel()
        collectJob = serviceScope.launch {
            AppComponent.INSTANCE.domain.serviceNotificationView.state.collect { view ->
                val title = view.title.ifBlank {
                    getString(R.string.sync_notification_channel)
                }
                val notification = buildNotification(title)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun stopCollecting() {
        collectJob?.cancel()
        collectJob = null
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.sync_notification_channel))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.sync_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.tsourcecode.wiki.app.notification.START"
        const val ACTION_STOP = "com.tsourcecode.wiki.app.notification.STOP"
        private const val CHANNEL_ID = "repository_sync"
        private const val NOTIFICATION_ID = 10_001
    }
}
