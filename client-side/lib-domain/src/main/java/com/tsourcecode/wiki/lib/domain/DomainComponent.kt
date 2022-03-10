package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.documents.ActiveDocumentController
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.FileManagerModel
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentProvider
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import kotlinx.coroutines.GlobalScope
import java.io.File

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

    val changedFilesController = ChangedFilesController(
            changedFilesDir = File(platformDeps.filesDir, "changed_files"),
            platformDeps.persistentStorageProvider,
    )

    val docContentProvider = DocumentContentProvider(
            changedFilesController,
    )

    val activeDocumentController = ActiveDocumentController()

    val configScreenModel = ConfigScreenModel(
            projectsRepository,
            platformDeps,
            quickStatusController,
            workerScope,
            navigator,
    )

    val projectComponents = ProjectComponentProvider(
            platformDeps, quickStatusController, activeDocumentController, workerScope, changedFilesController, navigator,
    )

    val fileManagerModel = FileManagerModel(
            workerScope,
            navigator,
            projectComponents,
            quickStatusController,
    )
}
