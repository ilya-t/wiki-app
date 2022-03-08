package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URL

class ProjectsRepository(
        private val platformDeps: PlatformDeps,
) {
    private val defaultProject = Project(
            name = "notes",
            filesDir = platformDeps.filesDir,
            url = URL("http://duke-nucem:8181/"),
    )

    fun update(projects: List<Project>) {
    }

    private val _data = MutableStateFlow(listOf(defaultProject))
    val data: StateFlow<List<Project>> = _data
}