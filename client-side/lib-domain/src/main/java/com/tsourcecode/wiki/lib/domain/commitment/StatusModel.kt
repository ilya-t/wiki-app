package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.backend.CurrentRevisionInfoController
import com.tsourcecode.wiki.lib.domain.backend.RevisionInfo
import com.tsourcecode.wiki.lib.domain.backend.SyncJob
import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.util.Completion
import com.tsourcecode.wiki.lib.domain.util.ErrorReporter
import com.tsourcecode.wiki.lib.domain.util.asCompletion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.net.URI

class StatusModel(
    private val project: Project,
    private val backendController: BackendController,
    private val fileStatus: FileStatusProvider,
    private val worker: CoroutineScope,
    private val navigator: AppNavigator,
    private val projectStorage: KeyValueStorage,
    private val currentRevisionInfoController: CurrentRevisionInfoController,
    private val scope: CoroutineScope,
    private val errorReporter: ErrorReporter,
) {
    private val messageStorage = StoredPrimitive.string("commit_message", projectStorage)
    private val commitTextFlow = MutableStateFlow("")
    private val _statusFlow = MutableStateFlow(StatusViewModel())
    private val commitAction: () -> Unit = {
        worker.launch {
            val message = commitTextFlow.value
            commitTextFlow.value = ""
            if (backendController.commit(message)) {
                backendController.pullOrSync("after commit")
            }
        }
    }
    val statusFlow: StateFlow<StatusViewModel> = _statusFlow

    init {
        worker.launch {
            commitTextFlow.value = messageStorage.value ?: ""
        }

        worker.launch {
            combine(
                currentRevisionInfoController.state,
                fileStatus.statusFlow,
                commitTextFlow, transform = this@StatusModel::toStatusViewModel).collect {
                    _statusFlow.value = it
            }
        }
    }

    private fun rollback(fs: FileStatus): Completion {
        return scope.launch {
            val rollbackResult: Result<Unit> = backendController.rollback(relativePath=fs.path)
            if (rollbackResult.isSuccess && fs.status == Status.NEW) {
                val absFile = File(project.repo, fs.path)
                if (!absFile.exists()) {
                    errorReporter.report(FileNotFoundException(
                        "Rollback file not found: ${absFile.absolutePath}"))
                } else {
                    absFile.delete()
                }
            }
        }.asCompletion()
    }

    private fun toStatusViewModel(revision: RevisionInfo?, status: StatusResponse?, commitText: String): StatusViewModel {
        val items = mutableListOf<StatusViewItem>()
        if (status?.files?.isNotEmpty() == true) {
            items.add(
                StatusViewItem.CommitViewItem(
                    commitMessage = commitText,
                    itemsInfo = "Changed files: ${status.files.size}",
                    commitAction = if (commitText.isNotBlank()) {
                        commitAction
                    } else {
                        null
                    },
                    updateCommitText = { updateCommitText(it) },
                )
            )
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
        worker.launch {
            val lastSeenCommitText = commitTextFlow.value
            if (lastSeenCommitText.isNotEmpty() && backendController.commit(lastSeenCommitText)) {
                backendController.pullOrSync("after commit#2")
                updateCommitText("")
            }
        }
    }

    fun notifyCommitScreenOpened() {
        worker.launch {
            fileStatus.update()
        }
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

    fun sync(reason: String): SyncJob {
        return backendController.pullOrSync(reason)
    }

    fun notifyCommitScreenResumed() {
        sync("resume to commit screen")
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
