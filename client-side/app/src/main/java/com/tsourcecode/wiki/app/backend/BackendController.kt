package com.tsourcecode.wiki.app.backend

import android.content.Context
import android.os.Handler
import android.os.Looper
import retrofit2.Retrofit
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors


class BackendController(private val context: Context) {
    private val retrofit = Retrofit.Builder()
            .baseUrl("http://duke-nucem/")
            .build()
    private val backendApi: WikiBackendAPIs = retrofit.create(WikiBackendAPIs::class.java)
    init {
        Executors.newSingleThreadExecutor().execute {
            try {
                requestAndSave()
            } catch (e: Exception) {
                e.printStackTrace()
                //Log.that("Unhandled exception: ", e)
                Handler(Looper.getMainLooper()).post {
                    throw e
                }
            }
        }
    }

    private fun requestAndSave() {
        try {
            //Log.that("1. Requesting")
            //you can now get your file in the InputStream
            val response = backendApi.latestRevision().execute()
            //Log.that("  response: ${response.code()}")
            val input: InputStream = response.body()?.byteStream()?:return
            val file = context.filesDir.absolutePath + "/revision.zip"
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
            e.printStackTrace()
            //Log.that("Ooops!", e)
        }
    }
}