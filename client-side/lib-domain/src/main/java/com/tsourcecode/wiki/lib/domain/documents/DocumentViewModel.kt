package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.project.ProjectComponent

class DocumentViewModel(
    private val component: ProjectComponent,
    private val document: Document,
) {
    //TODO("prevent doing this on UI")
    fun getContent(): String {
        return component.docContentProvider.getContent(document)
    }

    fun save(content: String) {
        component.documentsController.save(document, content)
    }

    fun refresh() {
        component.backendController.sync()
    }

}
