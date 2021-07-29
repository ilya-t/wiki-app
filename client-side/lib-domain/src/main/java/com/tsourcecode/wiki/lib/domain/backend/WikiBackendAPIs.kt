package com.tsourcecode.wiki.lib.domain.backend

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
    @POST("/api/1/stage")
    fun stage(@Body files: List<FileStaging>): Call<ResponseBody>

    data class FileStaging(
            val path: String,
            val content: String,
    )
}