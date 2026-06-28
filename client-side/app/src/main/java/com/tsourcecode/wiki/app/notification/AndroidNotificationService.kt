package com.tsourcecode.wiki.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.NotificationService

class AndroidNotificationService(
    private val context: Context,
) : NotificationService {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var lastMessage: String? = null

    override fun postNotification(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return
        }
        if (lastMessage == trimmed) {
            return
        }
        lastMessage = trimmed
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.sync_notification_channel))
            .setContentText(trimmed)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.sync_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "repository_sync"
        private const val NOTIFICATION_ID = 10_000
    }
}
