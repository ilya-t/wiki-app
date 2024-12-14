package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.QuickStatus
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.backend.api.SyncApiPayload
import com.tsourcecode.wiki.lib.domain.commitment.FileStatusProvider
import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import com.tsourcecode.wiki.lib.domain.commitment.UnstagedResponse
import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.hashing.DirHash
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.hashing.FileHash
import com.tsourcecode.wiki.lib.domain.hashing.Hashable
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.util.CoroutineScopes
import com.tsourcecode.wiki.lib.domain.util.Logger
import com.tsourcecode.wiki.lib.domain.util.Threading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder

internal const val REVISION_ZIP_REPOSITORY_DIR = "repo"

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
    private val logger: Logger,
) {
    internal var fileStatusProvider: FileStatusProvider? = null
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

    fun sync(): SyncJob {
        return doSync(SyncContext(
            rollbackSpecs = RollbackSpecs(emptyList()),
            fullSync = needFullSync(),
        ))
        // rebuild hashes
        // receive what needs to be uploaded
    }

    private fun needFullSync(): Boolean {
        if (!project.repo.exists()) {
            return true
        }
        return false
    }

    private class SyncContext(
        val rollbackSpecs: RollbackSpecs,
        val fullSync: Boolean,
    )

    private fun doSync(syncContext: SyncContext): SyncJob {
        val sync = logger.fork("-sync: ")
        val job = SyncJob()
        scope.launch {
            _refreshFlow.compareAndSet(expect = false, update = true)
            try {
                val localRevision: String? = currentRevisionInfoController.state.value?.revision
                    ?.trimEnd('\n') // TODO: fix server-side
                val fullSync = syncContext.fullSync
                sync.log { "start! (fullSync: $fullSync local-rev: '$localRevision')" }

                quickStatusController.udpate(QuickStatus.SYNC, "calculating hashes")
                val files = if (fullSync) emptyList() else elementHashProvider.getHashes()
                if (!fullSync) {
                    if (localRevision != null) {
                        val filesWithoutRollbacks: List<FileHash> = files
                            .flatFilesList()
                            .filter {
                                val relPath: String = it.relativePath
                                syncContext.rollbackSpecs.files.none { f -> f.path == relPath }
                            }
                        if (!tryStageChanges(localRevision, filesWithoutRollbacks, sync)) {
                            return@launch
                        }
                    } else {
                        sync.log { "staging skipped! No revision provided" }
                    }
                }

                quickStatusController.udpate(QuickStatus.SYNC, "syncing with backend")
                requestLastRevisionSnapshot(files)?.let { snapshot ->
                    val serverRevision = snapshot.revision
                    sync.log { "received server revision: '$serverRevision'" }
                    quickStatusController.udpate(QuickStatus.DECOMPRESS)
                    val syncOutput = File(platformDeps.internalFiles, project.id + "/sync")
                    val syncedFiles = if (fullSync) File(syncOutput, REVISION_ZIP_REPOSITORY_DIR) else syncOutput
                    Decompressor.decompress(
                        zipFile = snapshot.zipFile.absolutePath,
                        outputDir = syncOutput.absolutePath,
                        logger = { m: String ->
                            sync.log { "decompress: '$m'" }
                            quickStatusController.udpate(QuickStatus.DECOMPRESS, m)
                        })
                    quickStatusController.udpate(QuickStatus.SYNC)
                    if (fullSync) {
                        project.repo.deleteRecursively()
                        val result = syncedFiles.copyRecursively(project.repo, overwrite = true)
                        syncedFiles.deleteRecursively()
                        if (!result) {
                            quickStatusController.error(
                                RuntimeException("Move failed ($syncedFiles -> ${project.repo})"))
                        } else {
                            sync.log { "Move completed ($syncedFiles -> ${project.repo})" }
                        }
                    } else {
                        if (localRevision == serverRevision) {
                            sync.log { "staging non-synced files!" }
//                            syncedFiles
//                                .walkTopDown()
//                                .asSequence()
//                                .mapNotNull { resolveDocument(it, syncDir) }
//                                .toList()
//                                .forEach {
//                                    if (isRollback) {
//                                        do sync
//                                    }
//                                    sync.log { "staging: ${it.relativePath}" }
//                                    stage(it)
//                                }
//                            fileStatusProvider?.update()
                        } else {
                            sync.log { "sync to new revision '$localRevision' -> '$serverRevision'" }
                        }

                        val syncRelativePath = File(syncedFiles, REVISION_ZIP_REPOSITORY_DIR)
                        syncRelativePath
                            .walkTopDown()
                            .asSequence()
                            .mapNotNull { syncedFile ->
                                val d = resolveDocument(syncedFile, syncRelativePath) ?: run {
                                    if (syncedFile.isFile) {
                                        sync.log { "no document found for $syncedFile" }
                                    }
                                    return@mapNotNull null
                                }
                                syncedFile to d
                            }
                            .toList()
                            .forEach { (backendRevision: File, localRevision: Document) ->
                                var resolution: String? = null
                                if (true || canUpdateWithBackendRevision(syncContext, localRevision)) {
                                    localRevision.file.parentFile.mkdirs()
                                    backendRevision.copyTo(localRevision.file, overwrite = true)
                                    resolution = "accepted from backend"
                                } else {
                                    resolution = "declined from backend, staged"
                                    stage(localRevision)
                                }
                                sync.log { "syncing file: ${localRevision.relativePath}: $resolution" }
                            }
                        fileStatusProvider?.update()
                    }
                    currentRevisionInfoController.bumpRevisionToLatest()
                    syncedFiles.deleteRecursively()

                    dirRevision = snapshot.zipFile.nameWithoutExtension
                    scope.launch(threading.main) {
                        projectObserver?.invoke(dirRevision, project.repo)
                        quickStatusController.udpate(QuickStatus.SYNCED, currentRevisionInfoController.state.value?.toComment() ?: "null")
                    }
                    sync.log { "completed!" }
                    scope.launch {
                        elementHashProvider.notifyProjectFullySynced()
                    }
                }
            } catch (e: Exception) {
                sync.log("ERROR: $e")
                e.printStackTrace()
                scope.launch(threading.main) {
                    quickStatusController.error(QuickStatus.SYNC, e)
                }
            }
            job.notifyCompleted()
            _refreshFlow.compareAndSet(expect = true, update = false)
        }

        return job
    }

    private suspend fun tryStageChanges(
        localRevision: String,
        files: List<FileHash>,
        logger: Logger): Boolean {

        val localStatus = WikiBackendAPIs.LocalStatus(
            localRevision, files
                .map {
                    WikiBackendAPIs.LocalStatus.FileHash(
                        it.relativePath,
                        it.hash,
                    )
                }
        )
        val response: Response<ResponseBody> = try {
            backendApi.showNotStaged(project.name, localStatus).execute()
        } catch (e: IOException) {
            e.printStackTrace()
            logger.log("Changes request failed: ${e.message}")
            quickStatusController.error(QuickStatus.STAGE, e)
            return false
        }

        if (response.code() != 200) {
            logger.log("Changes request failed with code: ${response.code()}")
            quickStatusController.error(QuickStatus.STAGE,
                RuntimeException("Staging failed with ${response.errorBody()?.string()}")
            )
            return false
        }

        val body = response.body()?.string() ?: throw IllegalStateException("Empty body received!")
        logger.log("Detecting non staged. request: $localStatus")
        logger.log("Detecting non staged. response: $body")
        val notStaged = Json.decodeFromString(UnstagedResponse.serializer(), body)

        notStaged.files.forEach {
            val file = File(project.dir, it)
            if (file.isDirectory) {
                return@forEach
            }

            if (!file.exists()) {
                val error = RuntimeException("File for staging did disappeared: $file")
                logger.log("ERROR: ${error.message}")
                quickStatusController.error(error)
                return@forEach
            }
            val d = Document(project.dir, origin = file)

            logger.log("Staging: $it (resolved to: $d)")
            if (!stage(d)) {
                logger.log("Staging '$it' failed!")
            } else {
                logger.log("Staged successfully: $it")
            }
        }

        quickStatusController.udpate(QuickStatus.STAGED)
        return true
    }

    private fun canUpdateWithBackendRevision(
        syncContext: SyncContext,
        localRevision: Document
    ): Boolean {
        //maybe always accept?
        if (!localRevision.file.exists()) {
            return true
        }
        val rollbackRequested = syncContext.rollbackSpecs.files
            .find { it.path == localRevision.relativePath } != null
        return rollbackRequested
    }

    private fun resolveDocument(fileFromSync: File, syncDir: File): Document? {
        if (fileFromSync.isDirectory) {
            return null
        }
        val projectFile = File(project.dir.absolutePath +
                fileFromSync.absolutePath.substring(startIndex = syncDir.absolutePath.length))

        return Document(project.dir, origin = projectFile)
    }

    private class RevisionSnapshot(
        val revision: String,
        val zipFile: File,
    )

    /**
     * Heavy way of sync useful as "first-time-sync".
     */
    private suspend fun requestLastRevisionSnapshot(files: List<Hashable>): RevisionSnapshot? {
        try {
            val response = if (files.isEmpty()) {
                backendApi.latestRevision(project.name).execute()
            } else {
                backendApi.sync(
                    project.name,
                    RequestBody.create(
                        "application/json; charset=utf-8".toMediaTypeOrNull(),
                        SyncApiPayload.toBody(files)
                    )
                ).execute()
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
            val f = File(file)
            return RevisionSnapshot(revision = f.nameWithoutExtension, zipFile = f)
        } catch (e: IOException) {
//            e.printStackTrace()
            throw e
        }
    }

    private fun stage(d: Document): Boolean {
        val b64 = com.tsourcecode.wiki.lib.domain.util.Base64.getEncoder().encodeToString(d.file.readBytes())
        return stage(d.relativePath, b64)
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

    suspend fun commit(message: String): Boolean {
        quickStatusController.udpate(QuickStatus.COMMIT)
        val response = backendApi.commit(project.name,
                WikiBackendAPIs.Commitment(message)
        ).execute()
        val success = response.code() == 200
        if (success) {
            quickStatusController.udpate(QuickStatus.COMMITED)
        } else {
            quickStatusController.error(
                QuickStatus.COMMITED,
                RuntimeException("Commit failed with ${response.errorBody()?.string()}")
            )
        }
        return success
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

    fun pullOrSync() {
        scope.launch {
            if (fileStatusProvider?.statusFlow?.value?.files?.isNotEmpty() == true) {
                sync()
            } else {
                if (pull()) {
                    sync()
                }
            }

        }

    }

    private fun pull(): Boolean {
        quickStatusController.udpate(QuickStatus.STATUS_UPDATE, "pulling")
        val response = backendApi.pull(project.name).execute()
        if (!response.isSuccessful) {
            quickStatusController.error(
                QuickStatus.SYNC,
                IOException("error code(${response.code()}) with error: \n" +
                        response.errorBody()?.string()))
            return false
        }
        //TODO: maybe use revision from body?
        val body = response.body()?.string() ?: return false
        val revision = Json.decodeFromString(RevisionInfo.serializer(), body)
        quickStatusController.udpate(QuickStatus.STATUS_UPDATE, "Pulled to: ${revision.revision}")
        return true
    }

    fun rollback(relativePath: String) {
        scope.launch {
            val rollbackSpecs = RollbackSpecs(
                files = listOf(
                    FileRollback(
                        path = relativePath
                    )
                )
            )
            val response = backendApi.rollback(
                project.name, rollbackSpecs
            ).execute()
            if (!response.isSuccessful) {
                quickStatusController.error(
                    QuickStatus.STAGE,
                    IOException(
                        "error code(${response.code()}) with error: \n" +
                                response.errorBody()?.string()
                    )
                )
                return@launch
            }

            doSync(SyncContext(rollbackSpecs, fullSync = false))
        }
    }

    val Hashable.relativePath: String
        get() = this.file.absolutePath.substring(project.repo.absolutePath.length + 1)

}

private fun List<Hashable>.flatFilesList(): List<FileHash> {
    return this.flatMap {
        when (it) {
            is DirHash -> it.fileHashes.flatFilesList()
            is FileHash -> listOf(it)
        }
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
