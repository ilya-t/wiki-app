package com.tsourcecode.wiki.lib.domain.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface ProjectBackendAPIs {
    @GET("/api/1/projects")
    fun getProjects(): Call<ResponseBody>
}
/*
* /api/1/revision/show
* */
@Serializable
data class RevisionSpec(
    val revision: String,
)

@Serializable
data class RollbackSpecs(
    val files: List<FileRollback>,
)

@Serializable
data class FileRollback(
    val path: String,
)

@Serializable
data class RevisionInfo(
    @SerialName("revision")
    val revision: String,
    @SerialName("date")
    val date: String,
    @SerialName("message")
    val message: String,
) {
}
