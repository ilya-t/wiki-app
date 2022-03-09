package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URI

class ProjectsRepository(
        private val platformDeps: PlatformDeps,
) {
    private val defaultProject = Project(
            id = "notes_project",
            name = "notes",
            filesDir = platformDeps.filesDir,
            serverUri = URI("http://duke-nucem:8181/"),
            repoUri = "https://notes.git"
    )

    fun update(projects: List<Project>) {
        _data.value = projects
    }

    private val _data = MutableStateFlow(listOf(defaultProject))
    val data: StateFlow<List<Project>> = _data
}