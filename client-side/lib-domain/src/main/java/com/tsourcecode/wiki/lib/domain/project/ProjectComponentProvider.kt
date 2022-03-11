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
    fun get(p: Project): ProjectComponent {
        return components.getOrPut(p) {
            ProjectComponent(
                    p,
                    platformDeps,
                    quickStatusController,
                    workerScope,
                    changedFilesController,
                    navigator,
            )
        }

    }
}
