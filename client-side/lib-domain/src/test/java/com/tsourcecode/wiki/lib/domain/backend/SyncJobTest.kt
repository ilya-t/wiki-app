package com.tsourcecode.wiki.lib.domain.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test
import kotlin.system.measureTimeMillis

class SyncJobTest {
    @Test
    fun smoke() {
        val syncJob = SyncJob(CoroutineScope(Dispatchers.IO).async {
            delay(100)
            Result.success(Unit)
        })
        val duration = measureTimeMillis {
            syncJob.waitWithTimeout()
        }

        Assert.assertTrue("Expecting duration in specified range, instead got: $duration",
            duration > 100 && duration < 1000)
    }
}

private fun SyncJob.waitWithTimeout() {
    runBlocking {
        withTimeout(1000) {
            wait()
        }
    }
}
