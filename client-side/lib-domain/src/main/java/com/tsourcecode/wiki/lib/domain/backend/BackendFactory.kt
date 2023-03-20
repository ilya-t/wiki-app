package com.tsourcecode.wiki.lib.domain.backend

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

class BackendFactory(
    private val networkConfigurator: (OkHttpClient.Builder) -> OkHttpClient.Builder,
) {

    fun createProjectBackendAPIs(url: URL): ProjectBackendAPIs {
        val retrofit = Retrofit.Builder()
            .client(createOkHttpClient())
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ProjectBackendAPIs::class.java)
    }

    private fun createOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        return networkConfigurator.invoke(builder).build()
    }

    fun createWikiBackendApi(url: URL): WikiBackendAPIs {
        val retrofit = Retrofit.Builder()
            .client(createOkHttpClient())
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(WikiBackendAPIs::class.java)
    }
}