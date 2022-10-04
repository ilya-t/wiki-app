package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.documents.Element
import com.tsourcecode.wiki.app.documents.Folder
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DocumentsController(
    private val project: Project,
    private val backendController: BackendController,
    private val changedFilesController: ChangedFilesController,
) {
    private val _data = MutableStateFlow(Folder(project.repo, emptyList()))
    val data: StateFlow<Folder> = _data

    init {
        backendController.observeProjectUpdates { notifyProjectUpdated(it) }
    }

    private fun notifyProjectUpdated(dir: File) {
        GlobalScope.launch {
            if (!dir.isDirectory) {
                throw RuntimeException("Project dir($dir) is file!")
            }
            val folder = parseFolder(dir, dir)

            withContext(Dispatchers.Main) {
                _data.value = folder
            }
        }
    }

    private fun parseFolder(projectDir: File, dir: File): Folder {
        return Folder(
                dir,
                dir.safeListFiles()
                        .map { parseElement(projectDir, it) }
                        .sortedWith(FoldersFirst),
        )
    }


    private object FoldersFirst : Comparator<Element> {
        override fun compare(o1: Element, o2: Element): Int {
            val orderByType = o1.intType().compareTo(o2.intType())

            return if (orderByType == 0) {
                o1.file.name.compareTo(o2.file.name)
            } else {
                orderByType
            }
        }

        private fun Element.intType(): Int {
            return when (this) {
                is Folder -> 0
                is Document -> 1
            }
        }

    }

    private fun parseElement(projectDir: File, f: File): Element {
        return if (f.isDirectory) {
            parseFolder(projectDir, f)
        } else {
            Document(projectDir, f)
        }
    }
    //TODO: save doc not here but also on sync
    fun save(d: Document, content: String) {
        GlobalScope.launch {
            changedFilesController.markChanged(d, content)

            //TODO: appcompat this

            val b64 = com.tsourcecode.wiki.lib.domain.util.Base64.getEncoder().encodeToString(content.toByteArray())

            if (backendController.stage(d.relativePath, b64)) {
                changedFilesController.notifyFileSynced(d)
            }
        }
    }
}

private fun Folder.onEachDocument(action: (Document) -> Unit) {
    this.elements.forEach {
        when (it) {
            is Document -> action(it)
            is Folder -> it.onEachDocument(action)
        }
    }
}

private fun File.safeListFiles(): Array<File> {
    return this.listFiles() ?: emptyArray()
}