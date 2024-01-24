package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.backend.CurrentRevisionInfoController
import com.tsourcecode.wiki.lib.domain.backend.RevisionInfo
import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI

class StatusModel(
    private val project: Project,
    private val backendController: BackendController,
    private val fileStatus: FileStatusProvider,
    private val worker: CoroutineScope,
    private val navigator: AppNavigator,
    private val projectStorage: KeyValueStorage,
    private val currentRevisionInfoController: CurrentRevisionInfoController,
) {
    private val messageStorage = StoredPrimitive.string("commit_message", projectStorage)
    private val commitTextFlow = MutableStateFlow("")
    val statusFlow: Flow<StatusViewModel> = combine(
        currentRevisionInfoController.state,
        fileStatus.statusFlow,
        commitTextFlow, transform = this@StatusModel::toStatusViewModel)

    init {
        worker.launch {
            commitTextFlow.value = messageStorage.value ?: ""
        }
    }

    private fun rollback(fs: FileStatus) {
        backendController.rollback(relativePath=fs.path)
    }

    private fun toStatusViewModel(revision: RevisionInfo?, status: StatusResponse?, commitText: String): StatusViewModel {
        val items = mutableListOf<StatusViewItem>()
        if (status?.files?.isNotEmpty() == true) {
            items.add(StatusViewItem.CommitViewItem(
                commitText,
                "Changed files: ${status.files.size}",
            ) { updateCommitText(it) })
        }
        status?.files?.forEach { fs: FileStatus ->
            items.add(
                StatusViewItem.FileViewItem(
                    fileStatus = fs,
                    onFileClick = { notifyItemClicked(fs) },
                    onRollbackClick = { rollback(fs) },
                )
            )
        }
        revision?.toMessage()?.let {
            items.add(StatusViewItem.RevisionViewItem(it))
        }

        return StatusViewModel(
            items,
        )
    }

    fun updateCommitText(text: String) {
        commitTextFlow.value = text
        worker.launch {
            store(text)
        }
    }

    private fun store(commitText: String) {
        messageStorage.value = commitText
    }

    fun commit() {
        val lastSeenCommitText = commitTextFlow.value
        if (lastSeenCommitText.isNotEmpty()) {
            backendController.commit(lastSeenCommitText)
        }
    }

    fun notifyCommitScreenOpened() {
        fileStatus.update()
    }

    private fun notifyItemClicked(fileStatus: FileStatus) {
        val f = File(project.repo, fileStatus.path)
        val d = Document(
                projectDir = project.repo,
                origin = f,
        )

        if (f.exists()) {
            navigator.open(d.toNavigationURI(project))
        }
    }
}

private fun RevisionInfo?.toMessage(): String? {
    if (this == null) {
        return null
    }
    return "${this.revision} from (${this.date.replace("\n", "")})\n\n${this.message}"
}

internal fun Document.toNavigationURI(project: Project): URI {
    return URI("edit://${project.name}/${this.relativePath}")
}
