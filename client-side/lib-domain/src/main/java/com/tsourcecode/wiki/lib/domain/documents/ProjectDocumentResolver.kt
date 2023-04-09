package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.AppNavigator
import java.net.URI

class ProjectDocumentResolver(
    private val documentsController: DocumentsController,
) {
    fun resolve(uri: URI): Document? {
        val decodedPath = AppNavigator.extractDocumentPath(uri) ?: return null
        val element = documentsController.data.value.folder.find(decodedPath)
        if (element is Document) {
            return element
        }

        return null
    }
}