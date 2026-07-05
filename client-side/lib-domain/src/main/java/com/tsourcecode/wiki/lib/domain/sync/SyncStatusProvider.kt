package com.tsourcecode.wiki.lib.domain.sync

import com.tsourcecode.wiki.lib.domain.tests.OpenInTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OpenInTest
class SyncStatusProvider {
    private val _lastSync = MutableStateFlow<SyncData?>(null)
    val lastSync: StateFlow<SyncData?> = _lastSync.asStateFlow()

    fun beginSync(originRevision: Revision): SyncStatusMutator {
        val data = SyncData(
            originRevision = originRevision,
            targetRevision = null,
            logs = emptyList(),
            syncError = null,
        )
        update(
            data
        )

        return SyncStatusMutator(origin = data) {
            _lastSync.value = it
        }
    }

    private fun update(data: SyncData) {
        _lastSync.value = data
    }
}
