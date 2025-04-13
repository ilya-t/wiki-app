package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.util.Completion
import kotlinx.coroutines.Deferred

class SyncJob(private val job: Deferred<Result<Unit>>) : Completion {
    override suspend fun wait() {
        job.join()
    }

    suspend fun waitResults(): Result<Unit> {
        return job.await()
    }
}
