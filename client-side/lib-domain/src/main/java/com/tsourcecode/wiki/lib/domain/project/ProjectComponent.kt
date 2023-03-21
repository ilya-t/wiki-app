package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.backend.BackendFactory
import com.tsourcecode.wiki.lib.domain.backend.CurrentRevisionInfoController
import com.tsourcecode.wiki.lib.domain.backend.WikiBackendAPIs
import com.tsourcecode.wiki.lib.domain.commitment.FileStatusProvider
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.documents.staging.StagedFilesController
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.search.SearchModel
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class ProjectComponent(
        val project: Project,
        platformDeps: PlatformDeps,
        quickStatusController: QuickStatusController,
        workerScope: CoroutineScope,
        navigator: AppNavigator,
        storageProvider: PersistentStorageProvider,
        private val backendFactory: BackendFactory,
) {
    private val threading = platformDeps.threading

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

    val backendController = BackendController(
            platformDeps,
            quickStatusController,
            elementHashProvider,
            project,
            currentRevisionInfoController,
            wikiBackendAPIs,
            threading,
    )


    private val changedFiles = ChangedFilesController(
        project,
        workerScope,
    )

    private val stagedFiles = StagedFilesController(
        backendController,
        workerScope,
        projectStorage,
    )

    private val fileStatusProvider = FileStatusProvider(
            workerScope,
            changedFiles,
            stagedFiles,
    )

    val statusModel = StatusModel(
            project,
            backendController,
            fileStatusProvider,
            workerScope,
            navigator,
            projectStorage,
            currentRevisionInfoController,
    )

    val documentsController = DocumentsController(
            project,
            backendController,
            changedFiles,
            threading,
    )

    val searchModel = SearchModel(
            documentsController,
            workerScope,
            navigator,
            project,
    )

    val docContentProvider = DocumentContentProvider(
        changedFiles,
    )
}
