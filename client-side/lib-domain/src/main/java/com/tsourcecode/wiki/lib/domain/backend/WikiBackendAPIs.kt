package com.tsourcecode.wiki.lib.domain.backend

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

interface WikiBackendAPIs {
    @Streaming
    @GET("/api/1/revision/latest")
    fun latestRevision(): Call<ResponseBody>
    @Streaming
    @POST("/api/1/revision/sync")
    fun sync(@Body body: RequestBody): Call<ResponseBody>
    @POST("/api/1/stage")
    fun stage(@Body staging: Staging): Call<ResponseBody>
    @POST("/api/1/commit")
    fun commit(@Body c: Commitment): Call<ResponseBody>

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