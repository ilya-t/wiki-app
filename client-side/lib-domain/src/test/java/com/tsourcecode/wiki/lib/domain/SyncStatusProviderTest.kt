package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.sync.Revision
import com.tsourcecode.wiki.lib.domain.sync.SyncStatusProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncStatusProviderTest {
    @Test
    fun beginSyncResetsState() {
        val provider = SyncStatusProvider()
        val origin = Revision(revision = "rev1", date = "2024-01-01", message = "origin")

        provider.beginSync(origin)

        val data = provider.lastSync.value!!
        assertEquals(origin, data.originRevision)
        assertNull(data.targetRevision)
        assertEquals(emptyList<String>(), data.logs)
        assertNull(data.syncError)
    }

    @Test
    fun appendLogAddsMessages() {
        val provider = SyncStatusProvider()
        val mutator = provider.beginSync(emptyRevision())

        mutator.appendLog("first")
        mutator.appendLog("second")

        assertEquals(listOf("first", "second"), provider.lastSync.value!!.logs)
    }

    @Test
    fun completeSyncSetsTargetCommit() {
        val provider = SyncStatusProvider()
        val target = Revision(revision = "rev2", date = "2024-01-02", message = "done")
        val mutator = provider.beginSync(emptyRevision())

        mutator.completeSync(target)

        val data = provider.lastSync.value!!
        assertEquals(target, data.targetRevision)
        assertNull(data.syncError)
    }

    @Test
    fun failSyncSetsError() {
        val provider = SyncStatusProvider()
        val error = RuntimeException("sync failed")
        val mutator = provider.beginSync(emptyRevision())

        mutator.failSync(error)

        assertEquals(error, provider.lastSync.value!!.syncError)
    }

    private fun emptyRevision() = Revision(revision = "", date = "", message = "")
}
