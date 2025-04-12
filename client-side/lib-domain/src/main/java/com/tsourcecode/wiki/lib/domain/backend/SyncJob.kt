package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.util.Completion
import kotlinx.coroutines.Job

class SyncJob(private val job: Job) : Completion {
    override suspend fun wait() {
        job.join()
    }
}
