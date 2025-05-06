package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.serialization.json.Json

class ProjectAPIs(
    private val backendApi: WikiBackendAPIs,
    private val project: Project,
) {
    suspend fun fileStatus(): Result<StatusResponse> {
        val response = runCatching {
            backendApi.status(project.name).execute()
        }.getOrElse {
            return Result.failure(it)
        }
        if (response.code() != 200) {
            return Result.failure(
                RuntimeException("Status failed with ${response.errorBody()?.string()}")
            )
        }

        val body = response.body()?.string() ?: throw IllegalStateException("Empty body received!")
        val result = Json.decodeFromString(StatusResponse.serializer(), body)
        return Result.success(result)
    }

}