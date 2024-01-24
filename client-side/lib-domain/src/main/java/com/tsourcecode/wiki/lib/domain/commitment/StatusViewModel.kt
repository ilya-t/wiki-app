package com.tsourcecode.wiki.lib.domain.commitment

class StatusViewModel(
    val items: List<StatusViewItem> = listOf(),
)

sealed interface StatusViewItem {
    class CommitViewItem(
            val commitMessage: String = "",
            val itemsInfo: String,
            val updateCommitText: (text: String) -> Unit,
    ) : StatusViewItem

    class FileViewItem(
        val fileStatus: FileStatus,
        val onFileClick: () -> Unit,
        val onRollbackClick: () -> Unit,
    ) : StatusViewItem

    class RevisionViewItem(
            val message: String,
    ) : StatusViewItem
}
