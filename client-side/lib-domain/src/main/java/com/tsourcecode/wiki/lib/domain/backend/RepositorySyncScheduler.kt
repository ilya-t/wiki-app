package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.NotificationService
import com.tsourcecode.wiki.lib.domain.TaskScheduler
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentProvider
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import com.tsourcecode.wiki.lib.domain.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class RepositorySyncScheduler(
    private val projectsRepository: ProjectsRepository,
    private val projectComponentProvider: ProjectComponentProvider,
    private val workerScope: CoroutineScope,
    private val notificationService: NotificationService,
    taskScheduler: TaskScheduler,
) {
    init {
        taskScheduler.scheduleRecurrentJob {
            runBlocking {
                workerScope
                    .launch { syncAllProjects() }
                    .join()
            }
        }
    }

    private suspend fun syncAllProjects() {
        for (project in projectsRepository.data.value) {
            val component = projectComponentProvider.get(project)
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
                    notificationService.postNotification(
                        "${project.name} sync failed: ${it.message ?: it.javaClass.simpleName}"
                    )
                }
        }
    }
}
