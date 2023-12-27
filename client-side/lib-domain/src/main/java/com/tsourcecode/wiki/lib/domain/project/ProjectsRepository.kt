package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI

private const val STORAGE_KEY = "projects"

class ProjectsRepository(
        private val platformDeps: PlatformDeps,
        private val worker: CoroutineScope,
) {
    private val projectsStorage = platformDeps.persistentStorageProvider.get("projects")
    private val _data = MutableStateFlow(emptyList<Project>())
    val data: StateFlow<List<Project>> = _data

    init {
        worker.launch {
            val projects = projectsStorage.all.values.map {
                val sp = Json.decodeFromString(SerializableProject.serializer(), it)
                Project(
                        sp.id,
                        sp.name,
                        platformDeps.filesDir(),
                        URI(sp.serverUrl),
                        sp.repoUrl,
                )
            }
            _data.value = projects
        }
    }

    fun update(projects: List<Project>) {
        _data.value = projects
        worker.launch {
            val m = mutableMapOf<String, String>()
            projects.forEach { m[it.id] = Json.encodeToString(
                    SerializableProject.serializer(), it.toSerializableProject()) }
            projectsStorage.store(m)
        }
    }
}

private fun Project.toSerializableProject(): SerializableProject {
    return SerializableProject(
            this.id,
            this.name,
            this.repoUri,
            this.serverUri.toString(),
    )
}

@Serializable
class SerializableProject(
        @SerialName("id")
        val id: String,
        @SerialName("name")
        val name: String,
        @SerialName("repoUrl")
        val repoUrl: String,
        @SerialName("serverUrl")
        val serverUrl: String,


)
