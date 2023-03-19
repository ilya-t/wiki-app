package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.serialization.json.Json
import java.io.IOException

class CurrentRevisionInfoController(
    private val project: Project,
    private val wikiBackendAPIs: WikiBackendAPIs,
) {
    var currentRevision: RevisionInfo? = null

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
