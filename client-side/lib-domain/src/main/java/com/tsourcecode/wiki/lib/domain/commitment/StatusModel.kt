package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class StatusModel(
        private val backendController: BackendController,
        private val worker: CoroutineScope,
) {
    private var lastSeenCommitText = ""
    private var lastSeenStatus: StatusResponse? = null

    fun updateCommitText(text: String) {
        lastSeenCommitText = text
//        worker.launch {
//            rebuildData()
//        }
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

    fun commit(text: String) {
        lastSeenCommitText = text
        backendController.commit(text)
    }

    fun updateStatus() {
        worker.launch {
            lastSeenStatus = backendController.status()
            rebuildData()
        }
    }

    private val _statusFlow = MutableStateFlow(StatusViewModel())
    val statusFlow: Flow<StatusViewModel> = _statusFlow
}
