package com.tsourcecode.wiki.lib.domain.mocking

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.buffer
import okio.source
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files

sealed interface ResponseMaker {
    operator fun invoke(request: Request): Response

    object ResponseOK : ResponseMaker {
        override fun invoke(request: Request): Response {
            return Response.Builder()
                .code(200)
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .message("OK")
                .body(ResponseBody.create("application/json".toMediaTypeOrNull(), ""))
                .build()
        }
    }

    class FileResponse(
        private val file: File,
        private val setup: (File) -> Unit = {},
        private val teardown: (File) -> Unit = {},
    ) : ResponseMaker {
        override fun invoke(request: Request): Response {
            setup(file)

            if (!file.exists()) {
                throw FileNotFoundException(file.absolutePath)
            }
            val source = file.source().buffer()
            val mediaType = file.detectMediaType()
            val responseBody = ResponseBody.create(mediaType, file.length(), source)
            val result = Response.Builder()
                .code(200)
                .header("Content-Disposition", "attachment; filename=${file.name}")
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .body(responseBody)
                .message("OK")
                .build()
            teardown(file)
            return result
        }

        @Throws(IOException::class)
        private fun File.detectMediaType(): MediaType? {
            var contentType: String? = Files.probeContentType(this.toPath())
            if (contentType == null) {
                contentType = "application/octet-stream" // fallback to binary data if content type cannot be detected
            }
            return contentType.toMediaTypeOrNull()
        }
    }

    class JsonResponse(private val jsonString: String) : ResponseMaker {
        override fun invoke(request: Request): Response {
            val mediaType = "application/json".toMediaTypeOrNull()
            val responseBody = ResponseBody.create(mediaType, jsonString)
            return Response.Builder()
                .code(200)
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .body(responseBody)
                .message("OK")
                .build()
        }

    }
}

class ApiResponseInterceptor(
    private val mapLocal: Map<String, ResponseMaker>,
    private val onInterception: (req: Request, res: Response) -> Unit,
) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        val url = request.url.toUrl().toString()
        val key = mapLocal.keys.firstOrNull { url.contains(it) }
            ?: throw IOException("No mocks available for '$url'")
        val responseProvider = mapLocal[key]!!
        val response = responseProvider.invoke(request)
        onInterception(request, response)
        return response
    }
}
