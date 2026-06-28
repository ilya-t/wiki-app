package com.tsourcecode.wiki.lib.domain

object NoOpTaskScheduler : TaskScheduler {
    override val registeredJobs: Iterable<() -> Unit> = emptyList()

    override fun scheduleRecurrentJob(job: () -> Unit) = Unit
}
