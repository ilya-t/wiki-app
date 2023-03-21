package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import java.net.URI
import java.net.URLDecoder

class DocumentViewModelResolver(
    private val projectComponentResolver: ProjectComponentResolver,
) {
    fun resolveDocumentViewModel(uri: URI): DocumentViewModel? {
        if (!AppNavigator.isDocumentEdit(uri)) {
            return null
        }

        val component = projectComponentResolver.tryResolve(uri) ?: return null
        val decodedPath = uri.path.removePrefix("/").split("/").joinToString("/") { URLDecoder.decode(it, "UTF-8") }
        val element = component.documentsController.data.value.find(decodedPath)
        if (element is Document) {
            return DocumentViewModel(
                component,
                element,)
        }

        return null
    }
}
