package com.tsourcecode.wiki.app.scheduler

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tsourcecode.wiki.app.AppComponent

class RepositorySyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        AppComponent.INSTANCE.androidTaskScheduler.registeredJobs.forEach {
            it.invoke()
        }
        return Result.success()
    }
}
