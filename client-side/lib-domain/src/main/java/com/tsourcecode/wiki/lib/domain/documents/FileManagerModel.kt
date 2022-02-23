package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.app.documents.Folder
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
) {
    private val _dataFlow = MutableStateFlow(Folder(project.repo, emptyList()))
    val dataFlow: Flow<Folder> = _dataFlow

    private val navigationStack = Stack<Folder>()

    init {
        workerScope.launch {
            documentsController.data.collect {
                val currentFolder = _dataFlow.value

                if (currentFolder.file == project.repo) {
                    _dataFlow.value = it
                }
            }
        }
    }

    fun navigateBackward(): Boolean {
        if (navigationStack.isEmpty()) {
            return false
        }

        navigationStack.pop().let {
            _dataFlow.value = it
        }
        return true
    }

    fun navigateTo(dst: Folder) {
        navigationStack.push(_dataFlow.value)
        _dataFlow.value = dst
    }
}
