package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.NotificationService
import com.tsourcecode.wiki.lib.domain.TaskScheduler
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentProvider
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import com.tsourcecode.wiki.lib.domain.util.Logger
import com.tsourcecode.wiki.lib.domain.util.Threading
import kotlinx.coroutines.withContext

class RepositoryBackgroundSyncController(
    private val projectsRepository: ProjectsRepository,
    private val projectComponentProvider: ProjectComponentProvider,
    private val threading: Threading,
    private val notificationService: NotificationService,
    taskScheduler: TaskScheduler,
    private val appLogger: Logger,
) {
    init {
        taskScheduler.scheduleRecurrentJob()
    }

    suspend fun syncAllProjects(): Map<String, Throwable> = withContext(threading.io) {
        val results = mutableMapOf<String, Throwable>()
        for (project in projectsRepository.data.value) {
            val component = projectComponentProvider.get(project)
            appLogger.log { "Periodic background sync started!" }
            val notificationLogger = Logger { msg ->
                notificationService.postNotification("${project.name}: ${msg.trim()}")
            }
            component.backendController
                .pullOrSync("periodic background sync", notificationLogger)
                .waitResults()
                .onSuccess {
                    val revision = component.currentRevisionInfoController.state.value
                    val comment = revision?.let {
                        "${it.revision} (${it.date.replace("\n", "")})"
                    } ?: ""
                    val text = if (comment.isBlank()) {
                        "${project.name} synced"
                    } else {
                        "${project.name} synced — $comment"
                    }
                    notificationService.postNotification(text)
                }
                .onFailure {
                    results["sync of '${project.name}' failed"] = it
                    notificationService.postNotification(
                        "${project.name} sync failed: ${it.message ?: it.javaClass.simpleName}"
                    )
                }
        }

        if (results.isEmpty()) {
            appLogger.log { "Periodic background sync finished!" }
        } else {
            appLogger.log { "Periodic background sync finished with failures: $results" }
        }
        results
    }
}
