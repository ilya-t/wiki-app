package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.commitment.FileStatusProvider
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.search.SearchModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class ProjectComponent(
        val project: Project,
        platformDeps: PlatformDeps,
        quickStatusController: QuickStatusController,
        workerScope: CoroutineScope,
        changedFilesController: ChangedFilesController,
        navigator: AppNavigator,
) {
    private val elementHashProvider = ElementHashProvider(
            project,
            object : CoroutineScope {
                override val coroutineContext = Dispatchers.IO
            },
    )

    val backendController = BackendController(
            platformDeps,
            quickStatusController,
            elementHashProvider,
            project,
    )


    private val fileStatusProvider = FileStatusProvider(
            backendController,
            workerScope,
    )
    val statusModel = StatusModel(
            project,
            backendController,
            fileStatusProvider,
            workerScope,
            navigator,
    )

    val documentsController = DocumentsController(
            project,
            backendController,
            changedFilesController,
    )

    val searchModel = SearchModel(
            documentsController,
            workerScope,
            navigator,
            project,
    )
}