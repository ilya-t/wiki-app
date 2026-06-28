package com.tsourcecode.wiki.app.scheduler

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tsourcecode.wiki.lib.domain.TaskScheduler
import java.util.concurrent.TimeUnit

class AndroidTaskScheduler(
    private val context: Context,
) : TaskScheduler {
    private val _registeredJobs = mutableListOf<() -> Unit>()
    override val registeredJobs: List<() -> Unit> = _registeredJobs

    override fun scheduleRecurrentJob(job: () -> Unit) {
        _registeredJobs.add(job)
        val request = PeriodicWorkRequestBuilder<RepositorySyncWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        private const val WORK_NAME = "repository_sync"
    }
}
