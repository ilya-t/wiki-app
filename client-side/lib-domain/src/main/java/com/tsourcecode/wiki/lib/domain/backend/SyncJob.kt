package com.tsourcecode.wiki.lib.domain.backend

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class SyncJob {
    private val result = MutableSharedFlow<Unit>()

    suspend fun notifyCompleted() {
        result.emit(Unit)
    }

    suspend fun wait() {
        result.first()
    }
}
