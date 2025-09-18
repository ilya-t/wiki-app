package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.lib.domain.backend.ProjectAPIs
import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class StagedFilesController(
    private val workerScope: CoroutineScope,
    private val projectStorage: KeyValueStorage,
    private val projectAPIs: ProjectAPIs,
) {
    private val changesStorage = StoredPrimitive.string("staged_files", projectStorage)

    private val _stagedFiles = MutableStateFlow<StatusResponse?>(null)
    val data: Flow<StatusResponse> = _stagedFiles.filterNotNull()

    init {
        workerScope.launch {
            restore()
        }
    }

    private fun restore() {
        val changesJson = changesStorage.value ?: return
        _stagedFiles.value = Json.decodeFromString(StatusResponse.serializer(), changesJson)
    }

    suspend fun update() {
        projectAPIs.fileStatus().onSuccess { response: StatusResponse ->
            _stagedFiles.value = response
            store(response)
        }.onFailure {
            //TODO: handle error
        }
    }

    private fun store(changes: StatusResponse) {
        changesStorage.value = Json.encodeToString(StatusResponse.serializer(), changes)
    }
}
