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
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.util.CoroutineScopes
import com.tsourcecode.wiki.lib.domain.util.DebugLogger
import com.tsourcecode.wiki.lib.domain.util.Logger
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class DomainComponent<T : PlatformDeps>(
    val platformDeps: T,
    private val networkConfigurator: OkHttpClient.Builder.() -> OkHttpClient.Builder = { this },
) {
    private val scopes = CoroutineScopes(
        platformDeps.threading,
    )
    val navigator = AppNavigator(
        StoredPrimitive.string(
            "navigator_uri",
            platformDeps.persistentStorageProvider.getKeyValueStorage("navigator"),
        ),
    )
    val projectsRepository = ProjectsRepository(
        platformDeps,
        scopes.worker,
    )

    val quickStatusController = QuickStatusController()
    private val logger = Logger { message ->
        DebugLogger.log(message)
        scopes.main.launch {
            if (DebugLogger.inMemoryLogs.size > 10_000) {
                DebugLogger.inMemoryLogs.clear()
                DebugLogger.inMemoryLogs.add("auto-cleanup")
            }
            DebugLogger.inMemoryLogs.add(message)
        }
    }
    private val backendFactory = BackendFactory(
        logger,
        networkConfigurator)


    val projectComponents = ProjectComponentProvider(
        platformDeps,
        quickStatusController,
        navigator,
        backendFactory,
        scopes,
        logger,
    )

    val projectComponentResolver = ProjectComponentResolver(
        projectsRepository,
        projectComponents,
    )

    val viewModels: ViewModels = ViewModels(
        projectComponentResolver = projectComponentResolver,
        configScreenModel = ConfigScreenModel(
            projectsRepository,
            platformDeps,
            quickStatusController,
            scopes.worker,
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
        scopes.worker,
        platformDeps,
        navigator,
        projectComponentResolver,
    )
}
