package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatus
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.hashing.FileHashSerializable
import com.tsourcecode.wiki.lib.domain.hashing.Hashable
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.util.CoroutineScopes
import com.tsourcecode.wiki.lib.domain.util.Threading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder

private const val REVISION_ZIP_REPOSITORY_DIR = "repo"

class BackendController(
        private val platformDeps: PlatformDeps,
        private val quickStatusController: QuickStatusController,
        private val elementHashProvider: ElementHashProvider,
        private val project: Project,
        private val currentRevisionInfoController: CurrentRevisionInfoController,
        private val backendApi: WikiBackendAPIs,
        private val threading: Threading,
        private val scopes: CoroutineScopes,
        private val keyValueStorage: KeyValueStorage,
) {
    private val dirRevisionStorage = StoredPrimitive.string("dir_revision", keyValueStorage)
    private var projectObserver: ((String?, File) -> Unit)? = null
    private val _refreshFlow = MutableStateFlow(false)
    val refreshFlow: StateFlow<Boolean> = _refreshFlow
    private val scope = scopes.worker
    private var dirRevision: String? = dirRevisionStorage.value
        set(value) {
            field = value
            dirRevisionStorage.value = field
        }

    fun observeProjectUpdates(observer: (String?, File) -> Unit) {
        projectObserver = observer
        if (project.repo.exists()) {
            observer(dirRevision, project.repo)
        }
    }

    init {
        sync()
    }

    fun sync() {
        doSync(needFullSync())
        // rebuild hashes
        // receive what needs to be uploaded
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
                requestLastRevisionSnapshot(files)?.let { zipFile ->
                    currentRevisionInfoController.bumpRevisionToLatest()
                    quickStatusController.udpate(QuickStatus.DECOMPRESS)
                    val syncOutput = File(platformDeps.internalFiles, project.id + "/sync")
                    val syncedFiles = if (fullSync) File(syncOutput, REVISION_ZIP_REPOSITORY_DIR) else syncOutput
                    Decompressor.decompress(zipFile, syncOutput.absolutePath)

                    if (fullSync) {
                        project.repo.deleteRecursively()
                        syncedFiles.renameTo(project.repo)
                    } else {
                        val f = File(syncedFiles, REVISION_ZIP_REPOSITORY_DIR)
                        if (f.exists()) {
                            f.copyRecursively(project.repo, overwrite = true)
                        }
                    }

                    syncedFiles.deleteRecursively()

                    dirRevision = File(zipFile).nameWithoutExtension
                    scope.launch(threading.main) {
                        projectObserver?.invoke(dirRevision, project.repo)

                        quickStatusController.udpate(QuickStatus.SYNCED, currentRevisionInfoController.currentRevision.toComment())
                    }

                    scope.launch {
                        elementHashProvider.notifyProjectFullySynced()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scope.launch(threading.main) {
                    quickStatusController.error(QuickStatus.SYNC, e)
                }
            }
            _refreshFlow.compareAndSet(expect = true, update = false)
        }
    }

    /**
     * Heavy way of sync useful as "first-time-sync".
     */
    private suspend fun requestLastRevisionSnapshot(files: List<Hashable>): String? {
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
            val fileName = extractFileName(response.headers().get("Content-Disposition")?:"")
            val input: InputStream = response.body()?.byteStream() ?: return null
            val file = platformDeps.filesDir().absolutePath + "/" + fileName


            FileOutputStream(file, false).use { outputStream ->
                //Log.that("2. Saving to $file")
                var read: Int
                val bytes = ByteArray(DEFAULT_BUFFER_SIZE)
                while (input.read(bytes).also { read = it } != -1) {
                    outputStream.write(bytes, 0, read)
                }
                //Log.that("DONE!")
            }
            return file
        } catch (e: IOException) {
//            e.printStackTrace()
            throw e
        }
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

    @Throws(IOException::class)
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

private fun extractFileName(contentDisposition: String): String? {
    val filename = contentDisposition.substringAfter("filename=").trim().let {
        URLDecoder.decode(it, "UTF-8")
    }

    return filename
}

private fun RevisionInfo?.toComment(): String {
    if (this == null) {
        return ""
    }

    return " (${this.date.replace("\n", "")})\n${this.revision}\n${this.message.replace("\n","")}"
}
