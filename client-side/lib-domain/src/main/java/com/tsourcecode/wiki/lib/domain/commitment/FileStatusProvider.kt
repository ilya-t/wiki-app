package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.documents.staging.StagedFilesController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FileStatusProvider(
    private val workerScope: CoroutineScope,
    private val changedFiles: ChangedFilesController,
    private val stagedFiles: StagedFilesController,
    private val backendController: BackendController,
) {

    private val _statusFlow = MutableStateFlow<StatusResponse?>(
            null
    )

    val statusFlow: StateFlow<StatusResponse?> = _statusFlow

    init {
        backendController.fileStatusProvider = this
        workerScope.launch {
            stagedFiles.update()
            stagedFiles.data.combine(changedFiles.data) { staged, changed ->
                staged.extendWith(changed)
            }.collect {
                _statusFlow.value = it
            }
        }
    }

    fun update() {
        stagedFiles.update()
    }
}

private fun StatusResponse.extendWith(extra: StatusResponse): StatusResponse {
    val combination = linkedMapOf<String, FileStatus>()
    this.files.associateByTo(combination) { it.path }
    val extrasMap = extra.files.associateBy { it.path }
    combination.putAll(extrasMap)
    return StatusResponse(combination.values.toList())
}
