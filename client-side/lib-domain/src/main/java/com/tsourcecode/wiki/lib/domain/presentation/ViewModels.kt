package com.tsourcecode.wiki.lib.domain.presentation

import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.documents.DocumentViewModelResolver

class ViewModels(
    val configScreenModel: ConfigScreenModel,
    val documentViewModelResolver: DocumentViewModelResolver,
)
