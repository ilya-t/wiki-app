package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.backend.BackendFactory
import com.tsourcecode.wiki.lib.domain.util.CoroutineScopes

class ProjectComponentProvider(
        private val platformDeps: PlatformDeps,
        private val quickStatusController: QuickStatusController,
        private val navigator: AppNavigator,
        private val backendFactory: BackendFactory,
        private val scopes: CoroutineScopes,
) {
    private val components = mutableMapOf<Project, ProjectComponent>()
    private val requestedProjects = mutableMapOf<String, Project>()

    fun get(p: Project): ProjectComponent {
        val component = components.getOrPut(p) {
            ProjectComponent(
                    p,
                    platformDeps,
                    quickStatusController,
                    navigator,
                    platformDeps.persistentStorageProvider,
                    backendFactory,
                    scopes,
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
