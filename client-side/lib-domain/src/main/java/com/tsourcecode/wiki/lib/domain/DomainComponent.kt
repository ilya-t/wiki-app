package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import java.io.File
import java.net.URL
import kotlin.coroutines.CoroutineContext

class DomainComponent(
        private val platformDeps: PlatformDeps,
) {
    private val workerScope = GlobalScope
    private val defaultProject = Project(
            dir = File(platformDeps.filesDir.absolutePath + "/default_project"),
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
}