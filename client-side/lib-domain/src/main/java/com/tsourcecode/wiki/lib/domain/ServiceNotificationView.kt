package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.sync.SyncData
import com.tsourcecode.wiki.lib.domain.sync.SyncStatusProvider
import com.tsourcecode.wiki.lib.domain.util.CoroutineScopes
import com.tsourcecode.wiki.lib.domain.util.Threading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

class ServiceNotificationView(
    syncStatusProvider: SyncStatusProvider,
    scopes: CoroutineScopes,
) {
    private val _state = MutableStateFlow(NotificationView(title = "", desc = ""))
    val state: Flow<NotificationView> = _state.asStateFlow()

    init {
        scopes.worker.launch {
            syncStatusProvider.lastSync.filterNotNull().collect { syncData ->
                _state.value = syncData.toNotificationView()
            }
        }
    }
}

private fun SyncData.toNotificationView(): NotificationView {
    if (syncError != null) {
        return NotificationView(title = "Sync failed", desc = "${syncError.message}")
    }

    if (targetRevision != null) {
        val from = originRevision.date.replace("\n", "")
        val to = targetRevision.date.replace("\n", "")
        val msg = targetRevision.message.replace("\n", "")
        return NotificationView(title = "Sync completed", desc = "$from → $to\n$msg")
    }

    return NotificationView(
        title = "Syncing",
        desc = logs.lastOrNull() ?: ""
    )
}
