package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.AppNavigator
import java.net.URI

class ProjectComponentResolver(
        private val projectsRepository: ProjectsRepository,
        private val projectComponents: ProjectComponentProvider) {
    fun tryResolve(uri: URI): ProjectComponent? {
        val projectName = tryResolveProjectName(uri) ?: return null
        val project = projectsRepository.data.value.firstOrNull { it.name == projectName } ?: return null
        return projectComponents.get(project)
    }

    private fun tryResolveProjectName(uri: URI): String? {
        if (AppNavigator.isFileManagerNavigation(uri)) {
            return uri.host
        }

        if (AppNavigator.isDocumentEdit(uri)) {
            return uri.host
        }

        if (AppNavigator.isChanges(uri)) {
            val pathParts = uri.path.split("/")
            return pathParts[1]
        }

        if (AppNavigator.isSearch(uri)) {
            val pathParts = uri.path.split("/")
            return pathParts[1]
        }

        return null
    }
}