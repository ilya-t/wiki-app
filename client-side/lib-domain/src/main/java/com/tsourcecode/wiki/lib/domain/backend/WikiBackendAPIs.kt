package com.tsourcecode.wiki.lib.domain.backend

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface WikiBackendAPIs {
    @Streaming
    @GET("/{project}/api/1/revision/latest")
    fun latestRevision(@Path("project") name: String): Call<ResponseBody>
    @Streaming
    @POST("/{project}/api/1/revision/sync")
    fun sync(@Path("project") name: String, @Body body: RequestBody): Call<ResponseBody>
    @POST("/{project}/api/1/stage")
    fun stage(@Path("project") name: String, @Body staging: Staging): Call<ResponseBody>
    @POST("/{project}/api/1/status")
    fun status(@Path("project") name: String): Call<ResponseBody>
    @POST("/{project}/api/1/revision/show")
    fun showRevision(@Path("project") name: String, @Body spec: RevisionSpec): Call<ResponseBody>
    @POST("/{project}/api/1/commit")
    fun commit(@Path("project") name: String, @Body c: Commitment): Call<ResponseBody>
    @POST("/{project}/api/1/pull")
    fun pull(@Path("project") name: String): Call<ResponseBody>
    @POST("/{project}/api/1/rollback")
    fun rollback(@Path("project") project: String, @Body spec: RollbackSpecs): Call<ResponseBody>

    data class Staging(
            val files: List<FileStaging>
    )

    data class Commitment(
            val message: String,
            val user: String? = null,
            val email: String? = null,
    )

    data class FileStaging(
            val path: String,
            val content: String,
    )
}