package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.IOException

private const val KEY = "current_revision"

class CurrentRevisionInfoController(
    private val project: Project,
    private val wikiBackendAPIs: WikiBackendAPIs,
    private val keyValueStorage: KeyValueStorage,
) {
    private val _state = MutableStateFlow<RevisionInfo?>(readFromCache())
    val state: StateFlow<RevisionInfo?> = _state

    private fun readFromCache(): RevisionInfo? {
        keyValueStorage[KEY]?.let {
            return Json.decodeFromString(RevisionInfo.serializer(), it)
        }

        return null
    }

    fun bumpRevisionTo(info: RevisionInfo) {
        _state.value = info
        keyValueStorage[KEY] = Json.encodeToString(RevisionInfo.serializer(), info)
    }

    fun getRevisionInfo(revision: String): Result<RevisionInfo> {
        val response = try {
            wikiBackendAPIs.showRevision(project.name, RevisionSpec(revision)).execute()
        } catch (e: IOException) {
            return Result.failure(e)
        }
        if (!response.isSuccessful) {
            return Result.failure(
                IOException(
                    "showRevision failed with ${response.code()}: " +
                        response.errorBody()?.string()
                )
            )
        }
        val body = response.body()?.string()
            ?: return Result.failure(IOException("Empty body received!"))
        return runCatching {
            Json.decodeFromString(RevisionInfo.serializer(), body)
        }
    }
}
