package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController

class DocumentContentProvider(
    private val changedFilesController: ChangedFilesController,
) {
    fun getContent(d: Document): String {
        return d.file.readText()
    }
}
