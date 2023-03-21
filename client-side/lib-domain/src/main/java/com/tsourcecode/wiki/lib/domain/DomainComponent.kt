package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.backend.BackendFactory
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.documents.DocumentViewModelResolver
import com.tsourcecode.wiki.lib.domain.documents.FileManagerModel
import com.tsourcecode.wiki.lib.domain.navigation.InitialNavigationController
import com.tsourcecode.wiki.lib.domain.presentation.ViewModels
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentProvider
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import kotlinx.coroutines.GlobalScope
import okhttp3.OkHttpClient

class DomainComponent(
    platformDeps: PlatformDeps,
    private val networkConfigurator: (OkHttpClient.Builder) -> OkHttpClient.Builder = { it },
) {
    private val workerScope = GlobalScope
    val navigator = AppNavigator()
    val projectsRepository = ProjectsRepository(
            platformDeps,
            workerScope,
    )

    val quickStatusController = QuickStatusController()
    private val backendFactory = BackendFactory(networkConfigurator)

    val projectComponents = ProjectComponentProvider(
        platformDeps,
        quickStatusController,
        workerScope,
        navigator,
        backendFactory,
    )

    val projectComponentResolver = ProjectComponentResolver(
        projectsRepository,
        projectComponents,
    )

    val viewModels: ViewModels = ViewModels(
        configScreenModel = ConfigScreenModel(
            projectsRepository,
            platformDeps,
            quickStatusController,
            workerScope,
            navigator,
            backendFactory,
        ),
        documentViewModelResolver = DocumentViewModelResolver(
            projectComponentResolver,
        )
    )

    val fileManagerModel = FileManagerModel(
            navigator,
            quickStatusController,
    )

    private val initialNavigationController = InitialNavigationController(
        workerScope,
        platformDeps,
        navigator,
        projectComponentResolver,
    )
}
