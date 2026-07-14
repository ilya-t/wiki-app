package com.tsourcecode.wiki.lib.domain

object NoOpTaskScheduler : TaskScheduler {
    override fun scheduleRecurrentJob() = Unit
}
