package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ChangedFilesProvider {
    private val _changedFiles = MutableStateFlow(StatusResponse(emptyList()))
    val data: Flow<StatusResponse> = _changedFiles
}