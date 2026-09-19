package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Lenient on purpose: the backend adds response fields over time and an older
 * client must not break on the ones it does not know yet.
 */
private val json = Json { ignoreUnknownKeys = true }

class ProjectAPIs(
    private val backendApi: WikiBackendAPIs,
    private val project: Project,
    private val conflictController: ConflictController,
) {
    suspend fun fileStatus(): Result<StatusResponse> {
        val response = try {
            backendApi.status(project.name).execute()
        } catch (e: IOException) {
            return Result.failure(e)
        }
        if (response.code() != 200) {
            return Result.failure(
                RuntimeException("Status failed with ${response.errorBody()?.string()}")
            )
        }

        val body = response.body()?.string() ?: throw IllegalStateException("Empty body received!")
        val result = json.decodeFromString(StatusResponse.serializer(), body)
        conflictController.update(project.name, result.conflictBranches)
        return Result.success(result)
    }

}