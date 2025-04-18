package com.tsourcecode.wiki.lib.domain.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URL

class ProjectBackendController(
    backedFactory: BackendFactory,
    url: URL,
) {
    private val api = backedFactory.createProjectBackendAPIs(url)

    fun getConfigs(): Result<List<ProjectConfig>> {
        var result: Result<List<ProjectConfig>>? = null
        repeat(3) {
            val r = runCatching {
                val response = api.getProjects().execute()
                val body = response.body()?.string() ?: throw IOException("Empty body received!")
                val result = Json.decodeFromString(Configs.serializer(), body)
                return Result.success(result.configs)
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
)