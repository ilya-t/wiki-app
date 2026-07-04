package com.tsourcecode.wiki.app.notification

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tsourcecode.wiki.lib.domain.ForegroundSyncService

class AndroidForegroundSyncService(
    private val context: Context,
) : ForegroundSyncService {
    override fun start() {
        val intent = Intent(context, SyncForegroundService::class.java).apply {
            action = SyncForegroundService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stop() {
        val intent = Intent(context, SyncForegroundService::class.java).apply {
            action = SyncForegroundService.ACTION_STOP
        }
        context.startService(intent)
    }
}
