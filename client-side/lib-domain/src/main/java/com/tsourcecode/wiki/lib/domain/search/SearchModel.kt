package com.tsourcecode.wiki.lib.domain.search

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.documents.Folder
import com.tsourcecode.wiki.lib.domain.documents.ActiveDocumentController
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class SearchModel(
        project: Project,
        private val documentsController: DocumentsController,
        private val searchScope: CoroutineScope,
        private val activeDocumentController: ActiveDocumentController,
) {
    private val _data = MutableStateFlow(SearchViewModel(results = listOf(
            DocumentSearchResult(Document(project.repo, f = File("/tmp/some/file.md"))),
            DocumentSearchResult(Document(project.repo, f = File("/tmp/some/file2.md"))),
            )
    ))

    val data: Flow<SearchViewModel> = _data

    fun search(request: String) {
        _data.value = _data.value.copy(searchRequest = request)
        searchScope.launch {
            _data.value = _data.value.copy(results = searchDocuments(request))
        }

    }

    private fun searchDocuments(request: String): List<SearchResultView> {
        val root = documentsController.data.value

        if (request.isEmpty()) {
            return emptyList()
        }

        val results = mutableListOf<SearchResultView>()
        searchFolder(request, root, results)
        return results
    }

    private fun searchFolder(request: String, folder: Folder,
                             results: MutableList<SearchResultView>) {
        folder.elements.forEach { element ->
            when (element) {
                is Document -> {
                    if (element.relativePath.contains(request)) {
                        results.add(DocumentSearchResult(element))
                    }
                }
                is Folder -> {
                    searchFolder(request, element, results)
                }
            }
        }
    }


    fun notifyItemClicked(item: DocumentSearchResult) {
        activeDocumentController.switch(item.document)
    }
}
