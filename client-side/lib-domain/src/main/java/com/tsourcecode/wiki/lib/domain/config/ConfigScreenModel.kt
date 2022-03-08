package com.tsourcecode.wiki.lib.domain.config

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.URL

class ConfigScreenModel(
        private val projectsRepository: ProjectsRepository,
        private val platformDeps: PlatformDeps,
) {
    private val _data = MutableStateFlow(listOf(
            ConfigScreenItem.ConfigElement(), ConfigScreenItem.ImportFrom()
    ))
    val data: Flow<List<ConfigScreenItem>> = _data

    fun edit(index: Int, element: ConfigScreenItem) {
        val currentList = ArrayList(_data.value)
        currentList[index] = when (val e = element) {
            is ConfigScreenItem.ConfigElement -> e.copy(submitEnabled = true)
            is ConfigScreenItem.ImportFrom -> e
        }

        _data.value = currentList
    }

    fun submitImport(item: ConfigScreenItem.ImportFrom) {
        //TODO()
    }

    private fun ConfigScreenItem.ConfigElement.toProject() = Project(
            this.projectName,
            platformDeps.filesDir,
            URL(this.projectUrl),
    )

    fun submit(item: ConfigScreenItem.ConfigElement) {
        if (item.projectName.isEmpty() ||
                item.projectUrl.isEmpty() ||
                item.serverAddress.isEmpty()) {
            return
        }

        val currentList = _data.value
        _data.value =
                currentList.subList(0, currentList.lastIndex) +
                        item.copy(submitButton = SubmitButton.APPLY, submitEnabled = false) +
                        ConfigScreenItem.ConfigElement()

        projectsRepository.update(
                currentList
                        .filterIsInstance<ConfigScreenItem.ConfigElement>()
                        .map { it.toProject() }
        )
    }
}

sealed interface ConfigScreenItem {
    data class ConfigElement(
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