package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.sync.SyncData
import com.tsourcecode.wiki.lib.domain.sync.SyncStatusProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SyncForegroundCoordinator(
    private val syncStatusProvider: SyncStatusProvider,
    private val activityForegroundState: ActivityForegroundState,
    private val foregroundSyncService: ForegroundSyncService,
    scope: CoroutineScope,
) {
    private var lastStatus: SyncData? = null

    init {
        scope.launch {
            syncStatusProvider.lastSync.collect {
                lastStatus = it
                updateForeground()
            }
        }
        activityForegroundState.isInForeground
            .onEach { updateForeground() }
            .launchIn(scope)
    }

    private fun updateForeground() {
        if (activityForegroundState.inForeground) {
            foregroundSyncService.stop(keepNotification = false)
            return
        }

        val status = lastStatus
        when {
            status?.isCompleted() == false -> foregroundSyncService.start()
            status != null -> foregroundSyncService.stop(keepNotification = true)
            else -> foregroundSyncService.stop(keepNotification = false)
        }
    }
}
