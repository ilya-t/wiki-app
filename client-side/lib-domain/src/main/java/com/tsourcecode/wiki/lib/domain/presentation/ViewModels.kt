package com.tsourcecode.wiki.lib.domain.presentation

import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.documents.DocumentViewModelResolver
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent

class ViewModels(
    val configScreenModel: ConfigScreenModel,
    val documentViewModelResolver: DocumentViewModelResolver,
) {
    fun searchScreenModel(p: ProjectComponent) = p.searchModel
}
