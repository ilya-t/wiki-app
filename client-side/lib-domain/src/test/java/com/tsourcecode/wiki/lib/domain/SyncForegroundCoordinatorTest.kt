package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.sync.Revision
import com.tsourcecode.wiki.lib.domain.sync.SyncStatusMutator
import com.tsourcecode.wiki.lib.domain.sync.SyncStatusProvider
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
        startSync()

        assertTrue(foregroundService.isRunning)
    }

    @Test
    fun stopsForegroundWhenActivityResumes() = runCoordinatorTest {
        activityState.setInForeground(false)
        startSync()
        activityState.setInForeground(true)

        assertFalse(foregroundService.isRunning)
    }

    @Test
    fun stopsForegroundWhenSyncCompletesWhilePaused() = runCoordinatorTest {
        activityState.setInForeground(false)
        startSync()
        completeSync()

        assertFalse(foregroundService.isRunning)
    }

    @Test
    fun doesNotStartForegroundWhileActivityInForeground() = runCoordinatorTest {
        startSync()

        assertFalse(foregroundService.isRunning)
    }

    @Test
    fun stopsForegroundWhenSyncErrorsWhilePaused() = runCoordinatorTest {
        activityState.setInForeground(false)
        startSync()
        failSync(RuntimeException("network failure"))

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
        val syncStatusProvider = SyncStatusProvider()
        val activityState = ActivityForegroundState()
        val foregroundService = RecordingForegroundSyncService()
        private var syncMutator: SyncStatusMutator? = null

        init {
            SyncForegroundCoordinator(
                syncStatusProvider,
                activityState,
                foregroundService,
                scope,
            )
        }

        fun startSync() {
            syncMutator = syncStatusProvider.beginSync(emptyRevision())
        }

        fun completeSync() {
            syncMutator?.completeSync(
                Revision(revision = "rev2", date = "2024-01-02", message = "done")
            )
        }

        fun failSync(error: Throwable) {
            syncMutator?.failSync(error)
        }

        private fun emptyRevision() = Revision(revision = "", date = "", message = "")
    }

    private class RecordingForegroundSyncService : ForegroundSyncService {
        var isRunning = false
            private set

        override fun start() {
            isRunning = true
        }

        override fun stop(keepNotification: Boolean) {
            isRunning = false
        }
    }
}
