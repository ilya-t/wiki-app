package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.backend.BackendFactory
import com.tsourcecode.wiki.lib.domain.backend.CurrentRevisionInfoController
import com.tsourcecode.wiki.lib.domain.backend.ProjectAPIs
import com.tsourcecode.wiki.lib.domain.backend.WikiBackendAPIs
import com.tsourcecode.wiki.lib.domain.commitment.FileStatusProvider
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import com.tsourcecode.wiki.lib.domain.documents.ProjectDocumentResolver
import com.tsourcecode.wiki.lib.domain.documents.RecentDocumentsProvider
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.documents.staging.StagedFilesController
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.search.SearchModel
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.util.CoroutineScopes
import com.tsourcecode.wiki.lib.domain.util.ErrorReporter
import com.tsourcecode.wiki.lib.domain.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class ProjectComponent(
    val project: Project,
    platformDeps: PlatformDeps,
    quickStatusController: QuickStatusController,
    navigator: AppNavigator,
    storageProvider: PersistentStorageProvider,
    private val backendFactory: BackendFactory,
    scopes: CoroutineScopes,
    logger: Logger,
) {
    private val errorReporter = ErrorReporter()
    private val threading = platformDeps.threading
    private val projectLogger: Logger = logger.fork("project: '${project.name}'")

    private val elementHashProvider = ElementHashProvider(
            project,
            object : CoroutineScope {
                override val coroutineContext = Dispatchers.IO
            },
    )

    private val wikiBackendAPIs = createWikiBackendApi()

    private fun createWikiBackendApi(): WikiBackendAPIs {
        return backendFactory.createWikiBackendApi(project.serverUri.toURL())
    }

    private val projectStorage: KeyValueStorage =
        storageProvider.getKeyValueStorage("${project.id}_prefs")

    val currentRevisionInfoController = CurrentRevisionInfoController(
        project,
        wikiBackendAPIs,
        projectStorage,
    )

    private val changedFiles = ChangedFilesController(
        projectStorage,
        elementHashProvider,
        logger,
    )

    private val projectAPIs = ProjectAPIs(
        wikiBackendAPIs,
        project,
    )

    private val stagedFiles = StagedFilesController(
        scopes.worker,
        projectStorage,
        projectAPIs,
    )

    private val fileStatusProvider = FileStatusProvider(
        scopes.worker,
        changedFiles,
        stagedFiles,
        logger,
    )

    val backendController = BackendController(
            platformDeps,
            quickStatusController,
            elementHashProvider,
            project,
            currentRevisionInfoController,
            wikiBackendAPIs,
            threading,
            scopes,
            projectStorage,
            projectLogger,
        fileStatusProvider,
    )

    val statusModel = StatusModel(
            project,
            backendController,
            fileStatusProvider,
            scopes.worker,
            navigator,
            projectStorage,
            currentRevisionInfoController,
            scopes.worker,
            errorReporter,
    )

    val documentsController = DocumentsController(
            project,
            backendController,
            changedFiles,
            threading,
            scopes,
            quickStatusController,
    )

    private val documentResolver = ProjectDocumentResolver(
        documentsController,
    )

    private val recentDocumentsProvider = RecentDocumentsProvider(
        navigator,
        scopes,
        StoredPrimitive.stringList("recent_documents", projectStorage),
        documentResolver,
    )

    internal val searchModel = SearchModel(
            documentsController,
            scopes.worker,
            navigator,
            project,
            recentDocumentsProvider,
    )

    val docContentProvider = DocumentContentProvider(
        changedFiles,
    )
}
