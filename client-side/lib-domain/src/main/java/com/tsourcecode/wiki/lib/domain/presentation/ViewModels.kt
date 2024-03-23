package com.tsourcecode.wiki.lib.domain.presentation

import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.documents.DocumentViewModelResolver
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import java.net.URI

class ViewModels(
    private val projectComponentResolver: ProjectComponentResolver,
    val configScreenModel: ConfigScreenModel,
    val documentViewModelResolver: DocumentViewModelResolver,
) {
    fun searchScreenModel(p: ProjectComponent) = p.searchModel
    fun statusScreenModel(projectName: String): StatusModel? =
        projectComponentResolver.tryResolve(URI("settings://changes/$projectName"))?.statusModel
}
