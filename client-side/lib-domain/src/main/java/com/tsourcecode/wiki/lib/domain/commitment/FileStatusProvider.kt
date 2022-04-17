package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class FileStatusProvider(
        private val backendController: BackendController,
        private val workerScope: CoroutineScope,
) {
    private val _statusFlow = MutableStateFlow(
            StatusResponse(emptyList())
    )

    val statusFlow: StateFlow<StatusResponse> = _statusFlow

    init {
        workerScope.launch {
            tryUpdateStatus()
        }
    }

    fun update() {
        workerScope.launch {
            tryUpdateStatus()
        }
    }

    private fun tryUpdateStatus() {
        val status = try {
            backendController.status()
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        _statusFlow.value = status
    }

}
