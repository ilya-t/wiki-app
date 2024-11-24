package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.util.Completion
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

class SyncJob : Completion {
    private val result = MutableSharedFlow<Unit>()

    suspend fun notifyCompleted() {
        result.emit(Unit)
    }

    override suspend fun wait() {
        result.first()
    }
}
