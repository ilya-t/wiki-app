package com.tsourcecode.wiki.lib.domain.integration_tests

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException

class NetworkInterceptorByFileExistence : Interceptor {
    private var remoteFiles: List<String> = emptyList()
    var projectDir: File? = null

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (blockRequest(request)) {
            throw IOException("Network blocked by test condition on ${request.url}")
        }
        return chain.proceed(request)
    }

    private fun blockRequest(request: Request): Boolean {
        if (remoteFiles.isEmpty()) {
            return false
        }
        if (!request.url.encodedPath.contains("/api/1/status")) {
            return false
        }
        val projectDir = projectDir ?: return false
        return remoteFiles.any { File(projectDir, it).exists() }
    }

    fun interruptStatusAfterOnFilesExists(remoteFiles: List<String>) {
        this.remoteFiles = remoteFiles
    }
}