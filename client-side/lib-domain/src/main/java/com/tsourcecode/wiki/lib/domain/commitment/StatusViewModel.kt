package com.tsourcecode.wiki.lib.domain.commitment

class StatusViewModel(
        val items: List<StatusViewItem> = listOf(StatusViewItem.CommitViewItem()),
)

sealed interface StatusViewItem {
    class CommitViewItem(
            val commitMessage: String = "",
    ) : StatusViewItem

    class FileViewItem(
            val fileStatus: FileStatus,
    ) : StatusViewItem
}
