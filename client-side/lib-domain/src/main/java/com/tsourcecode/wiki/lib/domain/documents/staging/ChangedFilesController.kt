package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.lib.domain.commitment.FileStatus
import com.tsourcecode.wiki.lib.domain.commitment.Status
import com.tsourcecode.wiki.lib.domain.commitment.StatusResponse
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ChangedFilesController(
    project: Project,
    worker: CoroutineScope,
) {
    private val changedFilesDir = File(project.dir, "changed_files")
    private val _changedFiles = MutableStateFlow(StatusResponse(emptyList()))

    val data: Flow<StatusResponse> = _changedFiles

    init {
        worker.launch {
            changedFilesDir.mkdirs()
            _changedFiles.value = scanChangedFiles()
        }
    }

    private fun scanChangedFiles(): StatusResponse {
        if (!changedFilesDir.isDirectory) {
            throw RuntimeException("Changed dir($changedFilesDir) is file!")
        }
        val files = mutableListOf<File>()
        scanFolder(changedFilesDir, files)
        return StatusResponse(files.map { it.toFileStatus() })
    }

    private fun scanFolder(dir: File, results: MutableList<File>) {
        dir.safeListFiles().forEach { file ->
            if (file.isDirectory) {
                scanFolder(file, results)
            } else {
                results.add(file)
            }
        }
    }

    private fun File.safeListFiles(): Array<File> {
        return this.listFiles() ?: emptyArray()
    }

    fun markChanged(d: Document, modifiedContent: String) {
        val changedFile = d.toChangedFile()
        changedFile.parentFile.mkdirs()
        if (!changedFile.parentFile.exists()) {
            throw RuntimeException("Unable to generate file directories structure: ${changedFile.parentFile}")
        }
        changedFile.writeText(modifiedContent)

        val changedFilesList = _changedFiles.value.files
        if (changedFilesList.find { it.path == d.relativePath } != null) {
            return
        }

        _changedFiles.value = StatusResponse(changedFilesList + changedFile.toFileStatus())
    }

    fun notifyFileSynced(d: Document) {
        d.toChangedFile().delete()
        val changedFilesList = _changedFiles.value.files
        val updatedFilesList = changedFilesList.filter { it.path == d.relativePath }
        if (updatedFilesList.size == changedFilesList.size) {
            return
        }

        _changedFiles.value = StatusResponse(updatedFilesList)
    }

    fun isChanged(d: Document): Boolean {
        return d.toChangedFile().exists()
    }

    fun getChangedFile(d: Document): File? {
        val candidate = d.toChangedFile()
        if (candidate.exists()) {
            return candidate
        }

        return null
    }

    private fun Document.toChangedFile(): File {
        return File(changedFilesDir, this.relativePath)
    }

    private fun File.toFileStatus(): FileStatus {
        return FileStatus(
            path = toRelativeString(changedFilesDir),
            status = Status.MODIFIED,
            diff = "<sync with backend to see diff>"
        )
    }
}
