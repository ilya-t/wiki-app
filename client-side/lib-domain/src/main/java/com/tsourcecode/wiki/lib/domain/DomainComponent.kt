package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.backend.BackendFactory
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.documents.DocumentViewModelResolver
import com.tsourcecode.wiki.lib.domain.documents.FileManagerModel
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
    private val logger = Logger { m ->
        val message = withTimePasssedBlock(m)
        DebugLogger.log(message)
        scopes.main.launch {
            if (DebugLogger.inMemoryLogs.size > 10_000) {
                DebugLogger.inMemoryLogs.clear()
                DebugLogger.inMemoryLogs.add("auto-cleanup")
            }
            DebugLogger.inMemoryLogs.add(message)
        }
    }

    @Volatile
    private var lastMessageTime: Long = 0
    private val timePassedSize = 6

    private fun withTimePasssedBlock(m: String): String {
        val time = System.currentTimeMillis()
        val timePassedSinceLastMessage = time - lastMessageTime
        if (timePassedSinceLastMessage <= 0L) {
            lastMessageTime = time
            return m
        }
        lastMessageTime = time

        var timePassed = if (timePassedSinceLastMessage > 1000 && false) {
            "{" + timePassedSinceLastMessage / 1000 + "s}"
        } else {
            "${timePassedSinceLastMessage}ms ".padStart(3)
        }

        timePassed = if (timePassed.length < timePassedSize) {
            timePassed.padStart(timePassedSize, ' ')
        } else {
            timePassed
        }
        return timePassed + m
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
}
