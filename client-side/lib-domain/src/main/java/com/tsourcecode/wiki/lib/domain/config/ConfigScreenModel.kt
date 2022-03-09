package com.tsourcecode.wiki.lib.domain.config

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.backend.ProjectBackendController
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException

class ConfigScreenModel(
        private val projectsRepository: ProjectsRepository,
        private val platformDeps: PlatformDeps,
        private val quickStatusController: QuickStatusController,
        private val workerScope: CoroutineScope,
) {
    private val _data = MutableStateFlow(emptyList<ConfigScreenItem>())
    val data: Flow<List<ConfigScreenItem>> = _data

    init {
        workerScope.launch {
            projectsRepository.data.collect { buildModelData(it) }
        }
    }

    private fun buildModelData(projects: List<Project>) {
        val currentList = ArrayList(_data.value)
        val importItem = currentList.filterIsInstance<ConfigScreenItem.ImportFrom>().firstOrNull()
                ?: ConfigScreenItem.ImportFrom()
        val newItem = ConfigScreenItem.EditableElement()
        val newList = mutableListOf<ConfigScreenItem>(importItem)
        newList.addAll(projects.map { ConfigScreenItem.PreviewElement(it) })
        newList.add(newItem)
        _data.value = newList
    }

    fun edit(item: ConfigScreenItem.ImportFrom) {
        val currentList = ArrayList(_data.value)
        val index = currentList.indexOfFirst { it is ConfigScreenItem.ImportFrom }
        currentList[index] = item
        _data.value = currentList
    }

    fun edit(index: Int, element: ConfigScreenItem.PreviewElement) {
        val currentList = ArrayList(_data.value)
        currentList[index] = element.toEditableElement()
        _data.value = currentList
    }

    fun edit(index: Int, element: ConfigScreenItem.EditableElement) {
        val currentList = ArrayList(_data.value)
        currentList[index] = element.copy(submitEnabled = true)
        _data.value = currentList
    }

    fun submitImport(item: ConfigScreenItem.ImportFrom) {
        workerScope.launch {
            val url = URI(item.projectUrl)
            val controller = ProjectBackendController(url.toURL())
            val importedProjects = controller.getConfigs().map {
                Project(
                        id = it.name,
                        name = it.name,
                        filesDir = platformDeps.filesDir,
                        serverUri = url,
                        repoUri = it.repoUrl,
                )
            }
            val existingProjects = projectsRepository.data.value
            val existingRepos = existingProjects
                    .asSequence()
                    .filter { it.serverUri == url }
                    .map { it.repoUri }
                    .toHashSet()
            val newProjects = importedProjects.filter { !existingRepos.contains(it.repoUri) }
            projectsRepository.update(
                    existingProjects + newProjects
            )
        }
    }

    private fun ConfigScreenItem.EditableElement.toProject() = Project(
            id = this.projectId ?: this.projectName,
            name = this.projectName,
            filesDir = platformDeps.filesDir,
            serverUri = URI(this.serverAddress),
            repoUri = this.projectUrl,
    )

    fun submit(item: ConfigScreenItem.EditableElement) {
        if (item.projectName.isEmpty() ||
                item.projectUrl.isEmpty() ||
                item.serverAddress.isEmpty()) {
            return
        }

        val project = try {
            item.toProject()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            quickStatusController.error(e)
            return
        }

        val currentList = projectsRepository.data.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == project.id }

        if (index >= 0) {
            currentList[index] = project
        } else {
            currentList.add(project)
        }

        projectsRepository.update(currentList)

    }
}

sealed interface ConfigScreenItem {
    data class PreviewElement(
            private val project: Project,
    ) : ConfigScreenItem {
        val projectName: String = project.name

        fun toEditableElement() = EditableElement(
                projectId = project.id,
                serverAddress = project.serverUri.toString(),
                projectName = project.name,
                projectUrl = project.repoUri.toString(),
                submitEnabled = true,
                submitButton = SubmitButton.APPLY,
        )
    }

    data class EditableElement(
            val projectId: String? = null,
            val projectUrl: String = "",
            val projectName: String = "",
            val serverAddress: String = "",
            val submitButton: SubmitButton = SubmitButton.ADD,
            val submitEnabled: Boolean = true,
    ) : ConfigScreenItem

    data class ImportFrom(
            val projectUrl: String = "",
    ) : ConfigScreenItem
}

enum class SubmitButton {
    APPLY,
    ADD,
}