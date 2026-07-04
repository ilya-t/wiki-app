package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SyncForegroundCoordinator(
    quickStatusController: QuickStatusController,
    private val activityForegroundState: ActivityForegroundState,
    private val foregroundSyncService: ForegroundSyncService,
    scope: CoroutineScope,
) {
    private var lastStatus: StatusInfo? = null

    init {
        quickStatusController.addListener { status ->
            lastStatus = status
            updateForeground()
        }
        activityForegroundState.isInForeground
            .onEach { updateForeground() }
            .launchIn(scope)
    }

    private fun updateForeground() {
        val status = lastStatus
        val syncing = status?.error == null && status?.status?.isBackendWorkInProgress() == true
        val shouldShow = syncing && !activityForegroundState.inForeground
        if (shouldShow) {
            foregroundSyncService.start()
        } else {
            foregroundSyncService.stop()
        }
    }
}

private fun QuickStatus.isBackendWorkInProgress(): Boolean = when (this) {
    QuickStatus.SYNC,
    QuickStatus.DECOMPRESS,
    QuickStatus.STATUS_UPDATE,
    QuickStatus.STAGE,
    QuickStatus.COMMIT,
    -> true
    QuickStatus.SYNCED,
    QuickStatus.STAGED,
    QuickStatus.COMMITED,
    QuickStatus.STATUS_UPDATED,
    QuickStatus.ERROR,
    -> false
}
