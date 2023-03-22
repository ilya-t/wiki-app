package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent
import com.tsourcecode.wiki.lib.domain.util.NavigationUtils
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import java.net.URI

class FileManagerModel(
        private val appNavigator: AppNavigator,
        private val quickStatusController: QuickStatusController,
) {
    fun open(project: Project, element: Element) {
        when (element) {
            is Document -> appNavigator.open(NavigationUtils.openDocument(project, element))
            is Folder -> openFolder(project, element)
        }.also { /*exhaustive*/ }
    }

    private fun openFolder(p: Project, f: Folder) {
        val relativePath = f.file.absolutePath.removePrefix(p.repo.absolutePath).removePrefix("/")
        appNavigator.open(URI("open://${p.name}/${relativePath}"))
    }

    fun notifyRootClicked() {
        appNavigator.open(AppNavigator.PROJECTS_URI)
    }

    suspend fun show(component: ProjectComponent, filePath: String, folderObserver: (Folder) -> Unit) {
        //TODO: observe data appearance!
        component.documentsController.data.map { it.folder }.collect {
            val target = it.find(filePath)

            if (target == null || target !is Folder) {
                quickStatusController.error(RuntimeException("File not found: $filePath"))
                return@collect
            }

            folderObserver.invoke(target)
        }
    }
}
