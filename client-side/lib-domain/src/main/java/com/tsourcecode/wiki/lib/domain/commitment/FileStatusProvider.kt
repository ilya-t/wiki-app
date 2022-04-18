package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException

class FileStatusProvider(
        private val backendController: BackendController,
        private val workerScope: CoroutineScope,
        private val projectStorage: KeyValueStorage,
) {
    private val changesStorage = StoredPrimitive.string("changed_files", projectStorage)

    private val _statusFlow = MutableStateFlow<StatusResponse?>(
            null
    )

    val statusFlow: StateFlow<StatusResponse?> = _statusFlow

    init {
        workerScope.launch {
            restore()
            tryUpdateStatus()
        }
    }

    private fun restore() {
        val changesJson = changesStorage.value ?: return
        _statusFlow.value = Json.decodeFromString(StatusResponse.serializer(), changesJson)
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
        store(status)
    }

    private fun store(changes: StatusResponse) {
        changesStorage.value = Json.encodeToString(StatusResponse.serializer(), changes)
    }
}
