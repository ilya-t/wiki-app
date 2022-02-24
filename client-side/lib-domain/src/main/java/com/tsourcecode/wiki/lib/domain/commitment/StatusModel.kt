package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class StatusModel(
        private val backendController: BackendController,
        private val fileStatus: FileStatusProvider,
        private val worker: CoroutineScope,
) {
    private var lastSeenCommitText = ""
    private var lastSeenStatus: StatusResponse? = null

    init {
        worker.launch {
            fileStatus.statusFlow.collect {
                lastSeenStatus = it
                rebuildData()
            }
        }
    }

    fun updateCommitText(text: String) {
        lastSeenCommitText = text
        worker.launch {
            rebuildData()
        }
    }

    private fun rebuildData() {
        val items = mutableListOf<StatusViewItem>()
        items.add(StatusViewItem.CommitViewItem(lastSeenCommitText))
        lastSeenStatus?.files?.forEach {
            items.add(StatusViewItem.FileViewItem(it))
        }
        _statusFlow.value = StatusViewModel(
                items,
        )
    }

    fun commit() {
        if (lastSeenCommitText.isNotEmpty()) {
            backendController.commit(lastSeenCommitText)
        }
    }

    fun notifyCommitScreenOpened() {
        fileStatus.update()
    }

    private val _statusFlow = MutableStateFlow(StatusViewModel())
    val statusFlow: Flow<StatusViewModel> = _statusFlow
}
