package com.tsourcecode.wiki.app.backend

import android.content.Context
import android.os.Handler
import android.os.Looper
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.RuntimeException
import java.util.concurrent.Executors


class BackendController(
        private val context: Context,
) {
    private var projectObserver: ((String) -> Unit)? = null
    private val retrofit = Retrofit.Builder()
            .baseUrl("http://duke-nucem:8181/")
            .build()
    private val backendApi: WikiBackendAPIs = retrofit.create(WikiBackendAPIs::class.java)
    private val defaultProjectDir = context.filesDir.absolutePath + "/default_project"

    fun observeProjectUpdates(observer: (String) -> Unit) {
        projectObserver = observer
        if (File(defaultProjectDir).exists()) {
            observer(defaultProjectDir + "/repo")
        }
    }

    init {

        Executors.newSingleThreadExecutor().execute {
            try {
                requestAndSave()?.let {
                    Decompressor.decompress(it, defaultProjectDir)
                    Handler(Looper.getMainLooper()).post {
                        projectObserver?.invoke(defaultProjectDir + "/repo")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //Log.that("Unhandled exception: ", e)
                Handler(Looper.getMainLooper()).post {
                    throw e
                }
            }
        }
    }

    private fun requestAndSave(): String? {
        val file = context.filesDir.absolutePath + "/revision.zip" //TODO: remove hardcode
        try {
            //Log.that("1. Requesting")
            val response = backendApi.latestRevision().execute()
            //Log.that("  response: ${response.code()}")
            val input: InputStream = response.body()?.byteStream() ?: return null

            FileOutputStream(file, false).use { outputStream ->
                //Log.that("2. Saving to $file")
                var read: Int
                val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
                while (input.read(bytes).also { read = it } != -1) {
                    outputStream.write(bytes, 0, read)
                }
                //Log.that("DONE!")
            }
        } catch (e: IOException) {
//            e.printStackTrace()
            throw e
        }

        return file
    }

    fun stage(relativePath: String, b64: String) {
        val response = backendApi.stage(listOf(
                WikiBackendAPIs.FileStaging(relativePath, b64)
        )).execute()

        if (response.code() != 200) {
            throw RuntimeException("Staging failed with $response")
        }
    }
}