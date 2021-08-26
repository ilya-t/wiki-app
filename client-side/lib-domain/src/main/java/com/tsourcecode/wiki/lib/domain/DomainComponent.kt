package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import java.io.File

class DomainComponent(
        private val platformDeps: PlatformDeps,
) {
    val quickStatusController = QuickStatusController()
    val backendController = BackendController(platformDeps, quickStatusController)
    val changedFilesController = ChangedFilesController(
            changedFilesDir = File(platformDeps.filesDir, "changed_files"),
            platformDeps.persistentStorageProvider,
    )
    val docContentProvider = DocumentContentProvider(
            changedFilesController,
    )
}