package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatus
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors

class BackendController(
        private val platformDeps: PlatformDeps,
        private val quickStatusController: QuickStatusController,
) {
    private var projectObserver: ((String) -> Unit)? = null
    private val retrofit = Retrofit.Builder()
            .baseUrl("http://duke-nucem:8181/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    private val backendApi: WikiBackendAPIs = retrofit.create(WikiBackendAPIs::class.java)
    private val defaultProjectDir = platformDeps.filesDir.absolutePath + "/default_project"

    fun observeProjectUpdates(observer: (String) -> Unit) {
        projectObserver = observer
        if (File(defaultProjectDir).exists()) {
            observer(defaultProjectDir + "/repo")
        }
    }

    init {
        sync()
    }

    fun sync() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                quickStatusController.udpate(QuickStatus.SYNC)
                requestAndSave()?.let {
                    quickStatusController.udpate(QuickStatus.DECOMPRESS)
                    Decompressor.decompress(it, defaultProjectDir)

                    GlobalScope.launch(Dispatchers.Main) {
                        projectObserver?.invoke(defaultProjectDir + "/repo")
                        quickStatusController.udpate(QuickStatus.SYNCED)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //Log.that("Unhandled exception: ", e)
                GlobalScope.launch(Dispatchers.Main) {
                    quickStatusController.error(QuickStatus.SYNC, e)
                }
            }

        }
    }

    private fun requestAndSave(): String? {
        val file = platformDeps.filesDir.absolutePath + "/revision.zip" //TODO: remove hardcode
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
        quickStatusController.udpate(QuickStatus.STAGE)

        val response = backendApi.stage(WikiBackendAPIs.Staging(
                listOf(
                        WikiBackendAPIs.FileStaging(relativePath, b64)
                )
        )
                ).execute()

        if (response.code() != 200) {
            quickStatusController.error(QuickStatus.STAGE,
                    RuntimeException("Staging failed with ${response.errorBody()?.string()}")
            )

        } else {
            quickStatusController.udpate(QuickStatus.STAGED)
        }
    }

    fun commit(message: String) {
        quickStatusController.udpate(QuickStatus.COMMIT)
        val response = backendApi.commit(
                WikiBackendAPIs.Commitment(message)
        ).execute()
        if (response.code() != 200) {
            quickStatusController.error(
                    QuickStatus.COMMITED,
                    RuntimeException("Commit failed with ${response.errorBody()?.string()}")
            )

        } else {
            quickStatusController.udpate(QuickStatus.COMMITED)
        }

    }
}