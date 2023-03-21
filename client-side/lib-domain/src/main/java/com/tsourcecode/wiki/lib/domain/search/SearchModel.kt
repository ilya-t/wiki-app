package com.tsourcecode.wiki.lib.domain.search

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.commitment.toNavigationURI
import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import com.tsourcecode.wiki.lib.domain.documents.Folder
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SearchModel(
        private val documentsController: DocumentsController,
        private val searchScope: CoroutineScope,
        private val navigator: AppNavigator,
        private val project: Project,
) {
    private val _data = MutableStateFlow(SearchViewModel())

    val data: Flow<SearchViewModel> = _data

    fun search(request: String) {
        val processed = request.trim().replace("\n", "")

        if (processed == _data.value.searchRequest) {
            return
        }
        _data.value = _data.value.copy(searchRequest = processed)
        searchScope.launch {
            _data.value = _data.value.copy(results = searchDocuments(processed))
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
        navigator.open(item.document.toNavigationURI(project))
    }

    fun notifySearchKeyTriggered(viewModel: SearchViewModel) {
        viewModel.results.firstOrNull()?.let { firstResult ->
            if (firstResult is DocumentSearchResult) {
                navigator.open(firstResult.document.toNavigationURI(project))
            }
        }
    }
}
