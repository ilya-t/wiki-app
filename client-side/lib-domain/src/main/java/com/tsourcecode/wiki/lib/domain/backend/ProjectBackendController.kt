package com.tsourcecode.wiki.lib.domain.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import java.io.IOException
import java.net.URL

class ProjectBackendController(
    backedFactory: BackendFactory,
    url: URL,
) {
    private val api = backedFactory.createProjectBackendAPIs(url)
    private val json = Json { encodeDefaults = true }

    fun getConfigs(): Result<List<ProjectConfig>> {
        var result: Result<List<ProjectConfig>>? = null
        repeat(3) {
            val r = try {
                val response = api.getProjects().execute()
                if (!response.isSuccessful) {
                    throw IOException(
                        "getProjects failed with ${response.code()}: " +
                            response.errorBody()?.string()
                    )
                }
                val body = response.body()?.string() ?: throw IOException("Empty body received!")
                val configs = json.decodeFromString(Configs.serializer(), body)
                Result.success(configs.configs)
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(IOException(e))
            }
            result = r

            if (r.isSuccess) {
                return r
            }
        }

        return result ?: Result.failure(
            IOException("Failed to fetch project configs after 3 attempts")
        )
    }

    fun updateConfig(config: ProjectConfig): Result<Unit> {
        return try {
            val body = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                json.encodeToString(ProjectConfig.serializer(), config),
            )
            val response = api.updateProject(config.name, body).execute()
            if (!response.isSuccessful) {
                return Result.failure(
                    IOException(
                        "updateProject failed with ${response.code()}: " +
                            response.errorBody()?.string()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IOException(e))
        }
    }
}

@Serializable
data class Configs(
        @SerialName("configs")
        val configs: List<ProjectConfig>
)

@Serializable
data class ProjectConfig(
        @SerialName("name")
        val name: String,
        @SerialName("repo_url")
        val repoUrl: String,
        @SerialName("repo_cmd_after_clone")
        val repoCmdAfterClone: String = "",
)