package com.tsourcecode.wiki.lib.domain

interface TaskScheduler {
    val registeredJobs: Iterable<() -> Unit>
    fun scheduleRecurrentJob(job: () -> Unit)
}
