package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException

class StagedFilesController(
    private val backendController: BackendController,
    private val workerScope: CoroutineScope,
    private val projectStorage: KeyValueStorage,
) {
    private val changesStorage = StoredPrimitive.string("staged_files", projectStorage)

    private val _stagedFiles = MutableStateFlow(StatusResponse(emptyList()))
    val data: Flow<StatusResponse> = _stagedFiles

    init {
        workerScope.launch {
            restore()
        }
    }

    private fun restore() {
        val changesJson = changesStorage.value ?: return
        _stagedFiles.value = Json.decodeFromString(StatusResponse.serializer(), changesJson)
    }

    fun update() {
        workerScope.launch {
            val status = try {
                backendController.status()
            } catch (e: IOException) {
                e.printStackTrace()
                return@launch
            }
            _stagedFiles.value = status
            store(status)
        }
    }

    private fun store(changes: StatusResponse) {
        changesStorage.value = Json.encodeToString(StatusResponse.serializer(), changes)
    }
}
