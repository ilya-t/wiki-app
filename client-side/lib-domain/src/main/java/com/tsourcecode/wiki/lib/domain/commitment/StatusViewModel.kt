package com.tsourcecode.wiki.lib.domain.commitment

class StatusViewModel(
    val items: List<StatusViewItem> = listOf(),
)

sealed interface StatusViewItem {
    data class CommitViewItem(
        val commitMessage: String = "",
        val itemsInfo: String,
        val updateCommitText: (text: String) -> Unit,
        val commitAction: (() -> Unit)?,
    ) : StatusViewItem

    data class FileViewItem(
        val fileStatus: FileStatus,
        val onFileClick: () -> Unit,
        val onRollbackClick: () -> Unit,
    ) : StatusViewItem

    data class RevisionViewItem(
        val message: String,
    ) : StatusViewItem
}
