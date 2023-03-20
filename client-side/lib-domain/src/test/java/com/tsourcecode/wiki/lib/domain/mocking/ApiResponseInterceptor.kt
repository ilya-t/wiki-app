package com.tsourcecode.wiki.lib.domain.mocking

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException


class ApiResponseInterceptor(
    private val mapLocal: Map<String, Response>,
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url().url().toString()
        return mapLocal[url] ?: throw IOException("No mocks available for '$url'")
    }
}