package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.documents.FileManagerModel
import com.tsourcecode.wiki.lib.domain.navigation.InitialNavigationController
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentProvider
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import kotlinx.coroutines.GlobalScope

class DomainComponent(
        platformDeps: PlatformDeps,
) {
    private val workerScope = GlobalScope
    val navigator = AppNavigator()
    val projectsRepository = ProjectsRepository(
            platformDeps,
            workerScope,
    )

    val quickStatusController = QuickStatusController()

    val configScreenModel = ConfigScreenModel(
            projectsRepository,
            platformDeps,
            quickStatusController,
            workerScope,
            navigator,
    )

    val projectComponents = ProjectComponentProvider(
            platformDeps,
            quickStatusController,
            workerScope,
            navigator,
    )

    val fileManagerModel = FileManagerModel(
            navigator,
            quickStatusController,
    )

    val projectComponentResolver = ProjectComponentResolver(
            projectsRepository,
            projectComponents,
    )

    private val initialNavigationController = InitialNavigationController(
        workerScope,
        platformDeps,
        navigator,
        projectComponentResolver,
    )
}
