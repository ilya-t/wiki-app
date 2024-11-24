package com.tsourcecode.wiki.lib.domain.util

import kotlinx.coroutines.Job

interface Completion {
    suspend fun wait()
}

fun Job.asCompletion(): Completion {
    val job = this
    return object : Completion {
        override suspend fun wait() = job.join()
    }
}
