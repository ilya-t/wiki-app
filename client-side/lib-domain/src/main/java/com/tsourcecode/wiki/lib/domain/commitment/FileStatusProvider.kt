package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class FileStatusProvider(
        private val backendController: BackendController,
        private val workerScope: CoroutineScope,
) {
    private val _statusFlow = MutableStateFlow(
            StatusResponse(emptyList())
    )

    val statusFlow: Flow<StatusResponse> = _statusFlow

    init {
        workerScope.launch {
            _statusFlow.value = backendController.status()
        }
    }

    fun update() {
        workerScope.launch {
            _statusFlow.value = backendController.status()
        }
    }

}
