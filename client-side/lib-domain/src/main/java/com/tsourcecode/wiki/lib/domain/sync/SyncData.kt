package com.tsourcecode.wiki.lib.domain.sync

data class SyncData(
    val originRevision: Revision,
    val targetRevision: Revision?,
    val logs: List<String>,
    val syncError: Throwable?,
) {
    fun isCompleted(): Boolean {
        return syncError != null || targetRevision != null
    }
}
