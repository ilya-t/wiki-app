package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController

class DocumentContentProvider(
    private val changedFilesController: ChangedFilesController,
) {
    fun getContent(d: Document): String {
        changedFilesController.getChangedFile(d)?.let {
            return it.readText()
        }

        return d.file.readText()
    }
}
