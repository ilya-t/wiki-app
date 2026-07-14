package com.tsourcecode.wiki.app.scheduler

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tsourcecode.wiki.app.AppComponent
import kotlinx.coroutines.runBlocking

class RepositorySyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val failures: Map<String, Throwable> = runBlocking {
            AppComponent.INSTANCE.domain.repositoryBackgroundSyncController.syncAllProjects()
        }

        if (failures.isEmpty()) {
            return Result.success()
        }

        val errorMessages = failures.mapValues { (_, error) ->
            error.message ?: error.javaClass.simpleName
        }
        return Result.failure(
            Data.Builder()
                .putAll(errorMessages)
                .build()
        )
    }
}
