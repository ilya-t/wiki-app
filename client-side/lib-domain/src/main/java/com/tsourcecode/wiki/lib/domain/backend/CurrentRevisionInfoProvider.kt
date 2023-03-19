package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import kotlinx.serialization.json.Json
import java.io.IOException

private const val KEY = "current_revision"

class CurrentRevisionInfoController(
    private val project: Project,
    private val wikiBackendAPIs: WikiBackendAPIs,
    private val keyValueStorage: KeyValueStorage,
) {
    var currentRevision: RevisionInfo? = readFromCache()
        set(value) {
            if (field == value) {
                return
            }
            field = value

            if (value != null) {
                keyValueStorage[KEY] = Json.encodeToString(RevisionInfo.serializer(), value)
            } else {
                keyValueStorage[KEY] = null
            }
        }

    private fun readFromCache(): RevisionInfo? {
        keyValueStorage[KEY]?.let {
            return Json.decodeFromString(RevisionInfo.serializer(), it)
        }

        return null
    }

    fun bumpRevisionToLatest() {
        currentRevision = getRevisionInfo("HEAD~0")
    }

    @Throws(IOException::class)
    private fun getRevisionInfo(revision: String): RevisionInfo {
        val response = wikiBackendAPIs.showRevision(project.name, RevisionSpec(revision)).execute()
        val body = response.body()?.string() ?: throw IOException("Empty body received!")
        return Json.decodeFromString(RevisionInfo.serializer(), body)

    }
}
