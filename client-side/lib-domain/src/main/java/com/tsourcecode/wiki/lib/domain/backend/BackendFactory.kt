package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.util.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URL

class BackendFactory(
    logger: Logger,
    private val networkConfigurator: (OkHttpClient.Builder) -> OkHttpClient.Builder,
) {
    private val requestsLogger = logger.fork("OkHttp")

    fun createProjectBackendAPIs(url: URL): ProjectBackendAPIs {
        val retrofit = Retrofit.Builder()
            .client(createOkHttpClient())
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ProjectBackendAPIs::class.java)
    }

    private val loggingInterceptor = HttpLoggingInterceptor {
        requestsLogger.log(it)
    }.apply {
        setLevel(HttpLoggingInterceptor.Level.BASIC)
    }

    private fun createOkHttpClient(): OkHttpClient {

        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)

        return networkConfigurator.invoke(builder)
            .build()
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