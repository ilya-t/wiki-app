package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.commitment.FileStatusProvider
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.documents.ActiveDocumentController
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import com.tsourcecode.wiki.lib.domain.documents.FileManagerModel
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.search.SearchModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import java.io.File
import java.net.URL

class DomainComponent(
        private val platformDeps: PlatformDeps,
) {
    private val workerScope = GlobalScope
    val defaultProject = Project(
            name = "notes",
            filesDir = platformDeps.filesDir,
            url = URL("http://duke-nucem:8181/"),
    )
    val quickStatusController = QuickStatusController()
    private val elementHashProvider = ElementHashProvider(
            defaultProject,
            object : CoroutineScope {
                override val coroutineContext = Dispatchers.IO
            },
    )
    val backendController = BackendController(
            platformDeps,
            quickStatusController,
            elementHashProvider,
            defaultProject,
    )
    val changedFilesController = ChangedFilesController(
            changedFilesDir = File(platformDeps.filesDir, "changed_files"),
            platformDeps.persistentStorageProvider,
    )
    val docContentProvider = DocumentContentProvider(
            changedFilesController,
    )

    private val fileStatusProvider = FileStatusProvider(
            backendController,
            workerScope,
    )

    val activeDocumentController = ActiveDocumentController()

    val statusModel = StatusModel(
            defaultProject,
            backendController,
            fileStatusProvider,
            workerScope,
            activeDocumentController,
    )

    private val documentsController = DocumentsController(
            defaultProject,
            backendController,
            changedFilesController,
    )

    val fileManagerModel = FileManagerModel(
            defaultProject,
            documentsController,
            workerScope,
            fileStatusProvider,
    )

    val searchModel = SearchModel(
            documentsController,
            workerScope,
            activeDocumentController,
    )

    val configScreenModel = ConfigScreenModel(

    )
}