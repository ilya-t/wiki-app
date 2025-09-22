package com.tsourcecode.wiki.lib.domain.commitment

import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.documents.staging.StagedFilesController
import com.tsourcecode.wiki.lib.domain.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FileStatusProvider(
    private val workerScope: CoroutineScope,
    private val changedFiles: ChangedFilesController,
    private val stagedFiles: StagedFilesController,
    l: Logger,
) {
    private val logger: Logger = l.fork("-FileStatusProvider: ")

    private val _statusFlow = MutableStateFlow<StatusResponse?>(
            null
    )

    val statusFlow: StateFlow<StatusResponse?> = _statusFlow

    init {
        workerScope.launch {
            stagedFiles.update()
            stagedFiles.data.collect {
                logger.log {
                    "Staged files: $it"
                }
                _statusFlow.value = it
            }
        }
    }

    suspend fun update() {
        stagedFiles.update()
    }

    suspend fun getStagedFiles(): List<FileStatus> {
        return statusFlow.filterNotNull().first().files
    }

    suspend fun assumeCurrentProjectStateSynchronized() {
        changedFiles.assumeCurrentFilesAreSynchronized()
    }

    suspend fun hasChangedFiles(): Boolean = changedFiles.haveChanges()
}

private fun StatusResponse.extendWith(extra: StatusResponse): StatusResponse {
    val combination = linkedMapOf<String, FileStatus>()
    this.files.associateByTo(combination) { it.path }
    val extrasMap = extra.files.associateBy { it.path }
    combination.putAll(extrasMap)
    return StatusResponse(combination.values.toList())
}
