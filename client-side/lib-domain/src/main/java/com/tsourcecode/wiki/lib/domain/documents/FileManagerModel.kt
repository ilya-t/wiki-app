package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.documents.Element
import com.tsourcecode.wiki.app.documents.Folder
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.URI
import java.net.URLEncoder

class FileManagerModel(
        private val appNavigator: AppNavigator,
        private val quickStatusController: QuickStatusController,
) {
    private val _dataFlow = MutableStateFlow<ProjectFolder?>(null)
    val dataFlow: Flow<ProjectFolder?> = _dataFlow

    fun open(project: Project, element: Element) {
        when (element) {
            is Document -> openDocument(project, element)
            is Folder -> openFolder(project, element)
        }.also { /*exhaustive*/ }
    }

    private fun openDocument(p: Project, d: Document) {
        val encodedName = URLEncoder.encode(p.name, "UTF-8")
        val encodedPath = d.relativePath.split("/").map { URLEncoder.encode(it, "UTF-8") }.joinToString("/")
        appNavigator.open(URI("edit://${encodedName}/${encodedPath}"))
    }

    private fun openFolder(p: Project, f: Folder) {
        val relativePath = f.file.absolutePath.removePrefix(p.repo.absolutePath).removePrefix("/")
        appNavigator.open(URI("open://${p.name}/${relativePath}"))
    }

    fun notifyRootClicked() {
        appNavigator.open(AppNavigator.PROJECTS_URI)
    }

    fun show(component: ProjectComponent, filePath: String) {
        //TODO: observe data appearance!
        val root: Folder = component.documentsController.data.value
        val target: Element? = root.find(filePath)

        if (target == null || target !is Folder) {
            quickStatusController.error(RuntimeException("File not found: $filePath"))
            return
        }

        _dataFlow.value = ProjectFolder(component.project, target)

    }
}

class ProjectFolder(
        val project: Project,
        val folder: Folder,
)