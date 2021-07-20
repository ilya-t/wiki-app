package com.tsourcecode.wiki.app.backend

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Streaming

interface WikiBackendAPIs {
    @Streaming
    @GET("/api/1/revision/latest")
    fun latestRevision(): Call<ResponseBody>
}