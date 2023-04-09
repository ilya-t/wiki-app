package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import java.net.URI

class DocumentViewModelResolver(
    private val projectComponentResolver: ProjectComponentResolver,
) {
    fun resolveDocumentViewModel(uri: URI): DocumentViewModel? {
        val decodedPath = AppNavigator.extractDocumentPath(uri) ?: return null
        val component = projectComponentResolver.tryResolve(uri) ?: return null
        val element = component.documentsController.data.value.folder.find(decodedPath)
        if (element is Document) {
            return DocumentViewModel(
                component,
                element,)
        }

        return null
    }
}
