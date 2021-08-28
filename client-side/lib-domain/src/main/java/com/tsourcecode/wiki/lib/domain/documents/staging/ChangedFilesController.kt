package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider
import java.io.File

class ChangedFilesController(
        private val changedFilesDir: File,
        private val persistentStorageProvider: PersistentStorageProvider,
) {
    private val changedStorage = persistentStorageProvider.get("changed")
    private val changedFiles = mutableMapOf<String, String>().apply {
        putAll(changedStorage.all)
    }
    private val stagedStorage = persistentStorageProvider.get("staged")
    private val stagedFiles = mutableMapOf<String, String>().apply {
        putAll(stagedStorage.all)
    }

    init {
        changedFilesDir.mkdirs()
    }

    fun markChanged(d: Document, modifiedContent: String) {
        val changedFile = d.toChangedFile()
        changedFiles[d.relativePath] = changedFile.absolutePath
        changedStorage.store(changedFiles)

        stagedFiles.remove(d.relativePath)
        stagedStorage.store(stagedFiles)
        changedFile.writeText(modifiedContent)
    }

    fun markStaged(d: Document) {
        val stagedFile = d.toChangedFile()
        stagedFiles[d.relativePath] = stagedFile.absolutePath
        stagedStorage.store(stagedFiles)
    }

    fun notifyFileSynced(d: Document) {
        if (!changedFiles.contains(d.relativePath)) {
            return
        }

        if (!stagedFiles.contains(d.relativePath)) {
            return
        }

        d.toChangedFile().delete()
        changedFiles.remove(d.relativePath)
        stagedFiles.remove(d.relativePath)
        changedStorage.store(changedFiles)
        stagedStorage.store(changedFiles)
    }

    fun isChanged(d: Document): Boolean {
        return changedFiles.contains(d.relativePath)
    }

    fun isStaged(d: Document): Boolean {
        return stagedFiles.contains(d.relativePath)
    }

    fun getChangedFile(d: Document): File? {
        changedFiles[d.relativePath]?.let {
            return File(it)
        }

        return null
    }

    private fun Document.toChangedFile(): File {
        return File(changedFilesDir, this.relativePath)
    }
}

