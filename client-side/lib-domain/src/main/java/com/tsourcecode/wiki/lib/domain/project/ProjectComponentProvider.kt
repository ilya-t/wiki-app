package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.backend.BackendFactory
import kotlinx.coroutines.CoroutineScope

class ProjectComponentProvider(
        private val platformDeps: PlatformDeps,
        private val quickStatusController: QuickStatusController,
        private val workerScope: CoroutineScope,
        private val navigator: AppNavigator,
        private val backendFactory: BackendFactory,
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
                    navigator,
                    platformDeps.persistentStorageProvider,
                    backendFactory,
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
