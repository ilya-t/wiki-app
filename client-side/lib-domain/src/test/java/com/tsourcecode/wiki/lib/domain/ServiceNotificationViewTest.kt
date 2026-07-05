package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.sync.Revision
import com.tsourcecode.wiki.lib.domain.sync.SyncStatusProvider
import com.tsourcecode.wiki.lib.domain.util.CoroutineScopes
import com.tsourcecode.wiki.lib.domain.util.Threading
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceNotificationViewTest {
    @Test
    fun showsLastLogDuringInProgressSync() = runTest {
        val syncStatusProvider = SyncStatusProvider()
        val view = ServiceNotificationView(syncStatusProvider, testScopes())

        val mutator = syncStatusProvider.beginSync(
            Revision(revision = "rev1", date = "2024-01-01", message = "old message")
        )
        mutator.appendLog("syncing with backend")

        assertEquals(
            NotificationView(title = "Syncing", desc = "syncing with backend"),
            view.state.first(),
        )
    }

    @Test
    fun showsCommitDatesAndMessageOnSuccessfulSync() = runTest {
        val syncStatusProvider = SyncStatusProvider()
        val view = ServiceNotificationView(syncStatusProvider, testScopes())

        val mutator = syncStatusProvider.beginSync(
            Revision(revision = "rev1", date = "2024-01-01", message = "old message")
        )
        mutator.completeSync(
            Revision(revision = "rev2", date = "2024-01-02", message = "Updated docs")
        )

        assertEquals(
            NotificationView(
                title = "Sync completed",
                desc = "2024-01-01 → 2024-01-02\nUpdated docs",
            ),
            view.state.first(),
        )
    }

    @Test
    fun showsErrorOnFailedSync() = runTest {
        val syncStatusProvider = SyncStatusProvider()
        val view = ServiceNotificationView(syncStatusProvider, testScopes())

        val mutator = syncStatusProvider.beginSync(emptyRevision())
        mutator.appendLog("syncing with backend")
        mutator.failSync(RuntimeException("network failure"))

        assertEquals(
            NotificationView(title = "Sync failed", desc = "network failure"),
            view.state.first(),
        )
    }

    @Test
    fun showsErrorMessageWhenFailedSyncHasNoLogs() = runTest {
        val syncStatusProvider = SyncStatusProvider()
        val view = ServiceNotificationView(syncStatusProvider, testScopes())

        syncStatusProvider.beginSync(emptyRevision())
            .failSync(RuntimeException("network failure"))

        assertEquals(
            NotificationView(title = "Sync failed", desc = "network failure"),
            view.state.first(),
        )
    }

    private fun TestScope.testScopes(): CoroutineScopes {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return CoroutineScopes(
            object : Threading {
                override val main: CoroutineDispatcher = dispatcher
                override val io: CoroutineDispatcher = dispatcher
            }
        )
    }

    private fun emptyRevision() = Revision(revision = "", date = "", message = "")
}
