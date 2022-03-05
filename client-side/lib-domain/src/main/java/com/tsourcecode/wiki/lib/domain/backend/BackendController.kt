package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatus
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.hashing.FileHashSerializable
import com.tsourcecode.wiki.lib.domain.hashing.Hashable
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class BackendController(
        private val platformDeps: PlatformDeps,
        private val quickStatusController: QuickStatusController,
        private val elementHashProvider: ElementHashProvider,
        private val project: Project,
) {
    private var projectObserver: ((File) -> Unit)? = null
    private val retrofit = Retrofit.Builder()
            .baseUrl(project.url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    private val _refreshFlow = MutableStateFlow(false)
    val refreshFlow: StateFlow<Boolean> = _refreshFlow
    private val backendApi: WikiBackendAPIs = retrofit.create(WikiBackendAPIs::class.java)
    private val scope = GlobalScope

    fun observeProjectUpdates(observer: (File) -> Unit) {
        projectObserver = observer
        if (project.repo.exists()) {
            observer(project.repo)
        }
    }

    init {
        sync()
    }

    fun sync() {
        doSync(needFullSync())
    }

    private fun needFullSync(): Boolean {
        if (!project.repo.exists()) {
            return true
        }
        return false
    }

    private fun doSync(fullSync: Boolean) {
        scope.launch {
            _refreshFlow.compareAndSet(expect = false, update = true)
            try {
                quickStatusController.udpate(QuickStatus.SYNC)
                val files = if (fullSync) emptyList() else elementHashProvider.getHashes()
                requestLastRevisionSnapshot(files)?.let {
                    quickStatusController.udpate(QuickStatus.DECOMPRESS)
                    val syncOutput = File(project.dir.absolutePath + "/sync")
                    val syncedFiles = if (fullSync) File(syncOutput, "repo") else syncOutput
                    Decompressor.decompress(it, syncOutput.absolutePath)

                    if (fullSync) {
                        project.repo.deleteRecursively()
                        syncedFiles.renameTo(project.repo)
                    } else {
                        syncedFiles.copyRecursively(project.repo, overwrite = true)
                    }

                    syncedFiles.deleteRecursively()

                    scope.launch(Dispatchers.Main) {
                        projectObserver?.invoke(project.repo)
                        quickStatusController.udpate(QuickStatus.SYNCED)
                    }

                    scope.launch {
                        elementHashProvider.notifyProjectFullySynced()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scope.launch(Dispatchers.Main) {
                    quickStatusController.error(QuickStatus.SYNC, e)
                }
            }
            _refreshFlow.compareAndSet(expect = true, update = false)
        }
    }

    /**
     * Heavy way of sync useful as "first-time-sync".
     */
    private fun requestLastRevisionSnapshot(files: List<Hashable>): String? {
        val file = platformDeps.filesDir.absolutePath + "/revision.zip" //TODO: remove hardcode
        try {
            val response = if (files.isEmpty()) {
                backendApi.latestRevision(project.name).execute()
            } else {
                val hashes = FileHashSerializable.serializeList(files)
                val body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), hashes)
                backendApi.sync(project.name, body).execute()
            }
            //Log.that("1. Requesting")

            if (!response.isSuccessful) {
                quickStatusController.error(
                        QuickStatus.SYNC,
                        IOException("error code(${response.code()}) with error: \n" +
                                response.errorBody()?.string()))
                return null
            }
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

    fun stage(relativePath: String, b64: String): Boolean {
        quickStatusController.udpate(QuickStatus.STAGE)

        val response = try {
            backendApi.stage(project.name, WikiBackendAPIs.Staging(
                    listOf(WikiBackendAPIs.FileStaging(relativePath, b64))
            )).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            quickStatusController.error(QuickStatus.STAGE, e)
            return false
        }

        if (response.code() != 200) {
            quickStatusController.error(QuickStatus.STAGE,
                    RuntimeException("Staging failed with ${response.errorBody()?.string()}")
            )
            return false
        } else {
            quickStatusController.udpate(QuickStatus.STAGED)
            return true
        }
    }

    fun commit(message: String) {
        quickStatusController.udpate(QuickStatus.COMMIT)
        val response = backendApi.commit(project.name,
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

    fun status(): StatusResponse {
        quickStatusController.udpate(QuickStatus.STATUS_UPDATE)
        val response = backendApi.status(project.name).execute()
        if (response.code() != 200) {
            quickStatusController.error(
                    QuickStatus.STATUS_UPDATE,
                    RuntimeException("Status failed with ${response.errorBody()?.string()}")
            )
            return StatusResponse(emptyList())
        }

        val body = response.body()?.string() ?: throw IllegalStateException("Empty body received!")
        val result = Json.decodeFromString(StatusResponse.serializer(), body)
        quickStatusController.udpate(QuickStatus.STATUS_UPDATED)
        return result
    }
}
