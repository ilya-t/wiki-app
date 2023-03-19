package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.backend.CurrentRevisionInfoController
import com.tsourcecode.wiki.lib.domain.backend.ProjectBackendController
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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI

class ProjectComponent(
        val project: Project,
        platformDeps: PlatformDeps,
        quickStatusController: QuickStatusController,
        workerScope: CoroutineScope,
        navigator: AppNavigator,
        storageProvider: PersistentStorageProvider,
) {
    private val elementHashProvider = ElementHashProvider(
            project,
            object : CoroutineScope {
                override val coroutineContext = Dispatchers.IO
            },
    )

    private val wikiBackendAPIs = createWikiBackendApi()

    private fun createWikiBackendApi(): WikiBackendAPIs {
        val retrofit = Retrofit.Builder()
            .baseUrl(project.serverUri.toURL())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(WikiBackendAPIs::class.java)
    }

    val currentRevisionInfoController = CurrentRevisionInfoController(
        project,
        wikiBackendAPIs,
    )

    val backendController = BackendController(
            platformDeps,
            quickStatusController,
            elementHashProvider,
            project,
            currentRevisionInfoController,
            wikiBackendAPIs,
    )


    private val projectStorage: KeyValueStorage =
        storageProvider.getKeyValueStorage("${project.id}_prefs")

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
    )

    val documentsController = DocumentsController(
            project,
            backendController,
            changedFiles,
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
