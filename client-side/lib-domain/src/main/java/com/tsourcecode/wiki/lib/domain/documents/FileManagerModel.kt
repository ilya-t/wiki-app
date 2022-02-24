package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.app.documents.Folder
import com.tsourcecode.wiki.lib.domain.commitment.FileStatusProvider
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

class FileManagerModel(
        private val project: Project,
        private val documentsController: DocumentsController,
        private val workerScope: CoroutineScope,
        private val statusProvider: FileStatusProvider,
) {
    private val _dataFlow = MutableStateFlow(Folder(project.repo, emptyList()))
    val dataFlow: Flow<Folder> = _dataFlow

    private val navigationStack = Stack<Folder>()

    init {
        workerScope.launch {
            documentsController.data.collect {
                val currentFolder = _dataFlow.value

                if (currentFolder.file == project.repo) {
                    open(it)
                }
            }
        }
    }

    fun navigateBackward(): Boolean {
        if (navigationStack.isEmpty()) {
            return false
        }

        navigationStack.pop().let {
            open(it)

        }
        return true
    }

    private fun open(it: Folder) {
        _dataFlow.value = it
    }

    fun navigateTo(dst: Folder) {
        navigationStack.push(_dataFlow.value)
        open(dst)
    }
}
