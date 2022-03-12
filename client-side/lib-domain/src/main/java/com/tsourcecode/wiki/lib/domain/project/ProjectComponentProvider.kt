package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import kotlinx.coroutines.CoroutineScope

class ProjectComponentProvider(
        private val platformDeps: PlatformDeps,
        private val quickStatusController: QuickStatusController,
        private val workerScope: CoroutineScope,
        private val changedFilesController: ChangedFilesController,
        private val navigator: AppNavigator
) {
    private val components = mutableMapOf<Project, ProjectComponent>()
    private val requestedProjects = mutableMapOf<String, Project>()

    fun get(p: Project): ProjectComponent {
        val component = components.getOrPut(p) {
            ProjectComponent(
                    p,
                    platformDeps,
                    quickStatusController,
                    workerScope,
                    changedFilesController,
                    navigator,
            )
        }

        requestedProjects[p.name] = p
        return component
    }

    fun get(projectName: String): ProjectComponent? {
        requestedProjects[projectName]?.let {
            return get(it)
        }

        return null
    }
}
