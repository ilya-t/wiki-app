package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider
import java.io.File

class ChangedFilesController(
        private val changedFilesDir: File,
        private val persistentStorageProvider: PersistentStorageProvider,
) {
    private val storage = persistentStorageProvider.get("unstaged")
    private val changedFiles = mutableMapOf<String, String>().apply {
        putAll(storage.all)
    }

    init {
        changedFilesDir.mkdirs()
    }

    fun markChanged(d: Document, modifiedContent: String) {
        val changedFile = d.toChangedFile()
        changedFiles[d.relativePath] = changedFile.absolutePath
        storage.store(changedFiles)
        changedFile.writeText(modifiedContent)
    }

    fun markStaged(d: Document) {
        d.toChangedFile().delete()
        changedFiles.remove(d.relativePath)
        storage.store(changedFiles)
    }

    fun isChanged(d: Document): Boolean {
        return changedFiles.contains(d.relativePath)
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

