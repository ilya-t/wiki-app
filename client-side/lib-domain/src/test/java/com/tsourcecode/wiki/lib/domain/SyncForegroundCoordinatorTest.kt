package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncForegroundCoordinatorTest {
    @Test
    fun startsForegroundWhenSyncingAndActivityPaused() = runCoordinatorTest {
        activityState.setInForeground(false)
        quickStatusController.udpate(QuickStatus.SYNC, "syncing with backend")

        assertTrue(foregroundService.isRunning)
    }

    @Test
    fun stopsForegroundWhenActivityResumes() = runCoordinatorTest {
        activityState.setInForeground(false)
        quickStatusController.udpate(QuickStatus.SYNC, "syncing")
        activityState.setInForeground(true)

        assertFalse(foregroundService.isRunning)
    }

    @Test
    fun stopsForegroundWhenSyncCompletesWhilePaused() = runCoordinatorTest {
        activityState.setInForeground(false)
        quickStatusController.udpate(QuickStatus.SYNC, "syncing")
        quickStatusController.udpate(QuickStatus.SYNCED, "done")

        assertFalse(foregroundService.isRunning)
    }

    @Test
    fun doesNotStartForegroundWhileActivityInForeground() = runCoordinatorTest {
        quickStatusController.udpate(QuickStatus.SYNC, "syncing")

        assertFalse(foregroundService.isRunning)
    }

    @Test
    fun stopsForegroundWhenSyncErrorsWhilePaused() = runCoordinatorTest {
        activityState.setInForeground(false)
        quickStatusController.udpate(QuickStatus.SYNC, "syncing")
        quickStatusController.error(QuickStatus.SYNC, RuntimeException("network failure"))

        assertFalse(foregroundService.isRunning)
    }

    private fun runCoordinatorTest(block: CoordinatorTestContext.() -> Unit) = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val context = CoordinatorTestContext(scope)
            block(context)
        } finally {
            scope.cancel()
        }
    }

    private class CoordinatorTestContext(
        scope: CoroutineScope,
    ) {
        val quickStatusController = QuickStatusController()
        val activityState = ActivityForegroundState()
        val foregroundService = RecordingForegroundSyncService()

        init {
            SyncForegroundCoordinator(
                quickStatusController,
                activityState,
                foregroundService,
                scope,
            )
        }
    }

    private class RecordingForegroundSyncService : ForegroundSyncService {
        var isRunning = false
            private set

        override fun start() {
            isRunning = true
        }

        override fun stop() {
            isRunning = false
        }
    }
}
