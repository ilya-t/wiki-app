package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI

class StatusModel(
    private val project: Project,
    private val backendController: BackendController,
    private val fileStatus: FileStatusProvider,
    private val worker: CoroutineScope,
    private val navigator: AppNavigator,
    private val projectStorage: KeyValueStorage,
) {
    private val changesStorage = StoredPrimitive.string("changed_files", projectStorage)
    private val messageStorage = StoredPrimitive.string("commit_message", projectStorage)
    private var lastSeenCommitText = ""
    private var lastSeenStatus: StatusResponse? = null
    private val _statusFlow = MutableStateFlow(StatusViewModel())
    val statusFlow: StateFlow<StatusViewModel> = _statusFlow

    init {
        worker.launch {
            restore()
            rebuildData()
            fileStatus.statusFlow.filterNotNull().collect {
                lastSeenStatus = it
                rebuildData()
                store(lastSeenCommitText, lastSeenStatus)
            }
        }
    }

    fun updateCommitText(text: String) {
        lastSeenCommitText = text
        worker.launch {
            rebuildData()
            store(lastSeenCommitText, lastSeenStatus)
        }
    }

    private fun rebuildData() {
        val items = mutableListOf<StatusViewItem>()
        items.add(StatusViewItem.CommitViewItem(lastSeenCommitText))
        lastSeenStatus?.files?.forEach {
            items.add(StatusViewItem.FileViewItem(it))
        }
        val statusViewModel = StatusViewModel(
            items,
        )
        _statusFlow.value = statusViewModel
    }

    private fun store(commitText: String, changes: StatusResponse?) {
        messageStorage.value = commitText
        changesStorage.value = if (changes != null) {
            Json.encodeToString(StatusResponse.serializer(), changes)
        } else {
            null
        }
    }

    private fun restore() {
        lastSeenCommitText = messageStorage.value ?: ""
        val changesJson = changesStorage.value ?: return
        lastSeenStatus = Json.decodeFromString(StatusResponse.serializer(), changesJson)
    }

    fun commit() {
        if (lastSeenCommitText.isNotEmpty()) {
            backendController.commit(lastSeenCommitText)
        }
    }

    fun notifyCommitScreenOpened() {
        fileStatus.update()
    }

    fun notifyItemClicked(item: StatusViewItem.FileViewItem) {
        val f = File(project.repo, item.fileStatus.path)
        val d = Document(
                projectDir = project.repo,
                f = f,
        )

        if (f.exists()) {
            navigator.open(d.toNavigationURI(project))
        }
    }
}

internal fun Document.toNavigationURI(project: Project): URI {
    return URI("edit://${project.name}/${this.relativePath}")
}
