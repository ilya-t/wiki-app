package com.tsourcecode.wiki.lib.domain.backend

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface ProjectBackendAPIs {
    @GET("/api/1/projects")
    fun getProjects(): Call<ResponseBody>
}
