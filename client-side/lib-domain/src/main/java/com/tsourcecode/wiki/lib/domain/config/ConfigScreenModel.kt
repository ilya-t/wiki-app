package com.tsourcecode.wiki.lib.domain.config

import com.tsourcecode.wiki.lib.domain.AppNavigator
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
import java.util.*

class ConfigScreenModel(
        private val projectsRepository: ProjectsRepository,
        private val platformDeps: PlatformDeps,
        private val quickStatusController: QuickStatusController,
        private val workerScope: CoroutineScope,
        private val navigator: AppNavigator,
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
        val newItem = currentList.filterIsInstance<ConfigScreenItem.EditableElement>()
                .firstOrNull { it.origin == null } ?: ConfigScreenItem.PlusElement()
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
            id = this.origin?.id ?: UUID.randomUUID().toString(),
            name = this.projectName,
            filesDir = platformDeps.filesDir,
            serverUri = URI(this.serverAddress),
            repoUri = this.repoUrl,
    )

    fun submit(item: ConfigScreenItem.EditableElement) {
        if (item.projectName.isEmpty() ||
                item.repoUrl.isEmpty() ||
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

    fun addNewElement() {
        val currentList = ArrayList(_data.value)
        if (currentList[currentList.lastIndex] is ConfigScreenItem.PlusElement) {
            currentList[currentList.lastIndex] = ConfigScreenItem.EditableElement()
        } else {
            currentList.add(ConfigScreenItem.EditableElement())
        }
        _data.value = currentList
    }

    fun open(item: ConfigScreenItem.PreviewElement) {
        navigator.open(URI("open://${item.projectName}/"))
    }
}

sealed interface ConfigScreenItem {
    data class PreviewElement(
            private val project: Project,
    ) : ConfigScreenItem {
        val projectName: String = project.name

        fun toEditableElement() = EditableElement(
                origin = project,
                serverAddress = project.serverUri.toString(),
                projectName = project.name,
                repoUrl = project.repoUri,
                submitEnabled = true,
                submitButton = SubmitButton.APPLY,
        )
    }

    data class EditableElement(
            val origin: Project? = null,
            val repoUrl: String = "",
            val projectName: String = "",
            val serverAddress: String = "",
            val submitButton: SubmitButton = SubmitButton.ADD,
            val submitEnabled: Boolean = true,
    ) : ConfigScreenItem

    class PlusElement : ConfigScreenItem

    data class ImportFrom(
            val projectUrl: String = "",
    ) : ConfigScreenItem
}

enum class SubmitButton {
    APPLY,
    ADD,
}