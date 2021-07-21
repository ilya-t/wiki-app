package com.tsourcecode.wiki.app.backend

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.tsourcecode.wiki.app.DocumentsController
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors


class BackendController(
        private val context: Context,
        private val documentsController: DocumentsController,
) {
    private val retrofit = Retrofit.Builder()
            .baseUrl("http://duke-nucem:8181/")
            .build()
    private val backendApi: WikiBackendAPIs = retrofit.create(WikiBackendAPIs::class.java)
    private val defaultProjectDir = context.filesDir.absolutePath + "/default_project"
    init {
        if (File(defaultProjectDir).exists()) {
            documentsController.notifyProjectUpdated(defaultProjectDir+"/repo")
        }

        Executors.newSingleThreadExecutor().execute {
            try {
                requestAndSave()?.let {
                    Decompressor.decompress(it, defaultProjectDir)
                    Handler(Looper.getMainLooper()).post {
                        documentsController.notifyProjectUpdated(defaultProjectDir+"/repo")
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
            val input: InputStream = response.body()?.byteStream()?:return null

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
}