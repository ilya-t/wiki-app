package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController

class DocumentContentProvider(
    private val changedFilesController: ChangedFilesController,
) {
    private val inMemoryStore = mutableMapOf<Document, String>()

    fun getContent(d: Document): String {
        return inMemoryStore.getOrPut(d) {
            changedFilesController.getChangedFile(d)?.let {
                return@getOrPut it.readText()
            }

            return d.file.readText()
        }
    }
}
