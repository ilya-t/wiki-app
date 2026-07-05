package com.tsourcecode.wiki.lib.domain.sync

class SyncStatusMutator(
    origin: SyncData,
    private val updateEmitter: (SyncData) -> Unit
) {
    private var current: SyncData = origin

    fun appendLog(message: String) {
        mutate(current.copy(logs = current.logs + message))
    }

    private fun mutate(new: SyncData) {
        current = new
        updateEmitter(new)
    }

    fun completeSync(targetRevision: Revision) {
        mutate(current.copy(targetRevision = targetRevision, syncError = null))
    }

    fun failSync(error: Throwable) {
        mutate(current.copy(syncError = error))
    }

}
