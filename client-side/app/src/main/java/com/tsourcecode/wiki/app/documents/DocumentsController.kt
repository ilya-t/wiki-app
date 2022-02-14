package com.tsourcecode.wiki.lib.domain.documents

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.documents.Element
import com.tsourcecode.wiki.app.documents.Folder
import kotlinx.coroutines.*
import java.io.File
import java.lang.RuntimeException
import kotlin.Comparator

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tsourcecode.wiki.app.EditStateController
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController


class DocumentsController(
        private val backendController: BackendController,
        private val editStateController: EditStateController,
        private val changedFilesController: ChangedFilesController,
) {
    private val _data = MutableLiveData<Folder>()
    val data: LiveData<Folder> = _data

    init {
        backendController.observeProjectUpdates { notifyProjectUpdated(it) }
    }

    private fun notifyProjectUpdated(dir: File) {
        GlobalScope.launch {
            if (!dir.isDirectory) {
                throw RuntimeException("Project dir($dir) is file!")
            }
            val folder = parseFolder(dir, dir)
            folder.onEachDocument {
                changedFilesController.notifyFileSynced(it)
            }

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

    fun save(d: Document, content: String) {
        GlobalScope.launch {
            changedFilesController.markChanged(d, content)

            val b64 = Base64.encodeToString(content.toByteArray(), Base64.DEFAULT)

            if (backendController.stage(d.relativePath, b64)) {
                changedFilesController.markStaged(d)
            }

            withContext(Dispatchers.Main) {
                editStateController.enableCommit()
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

class DocumentsAdapter(
        private val docContentProvider: DocumentContentProvider,
) : RecyclerView.Adapter<DocumentViewHolder>() {
    var openDelegate: (Element) -> Unit = {}
    private val items = mutableListOf<Element>()
    fun update(newItems: List<Element>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            DocumentViewHolder(createItemView(parent), openDelegate, docContentProvider)

    private fun createItemView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(R.layout.doc_item, parent, false)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}

class DocumentViewHolder(
        itemView: View,
        private val openDelegate: (Element) -> Unit,
        private val docContentProvider: DocumentContentProvider
) : RecyclerView.ViewHolder(itemView) {
    private var boundedElement: Element? = null
    private val tvTitle = itemView.findViewById<AppCompatTextView>(R.id.tv_title)
    private val tvPreview = itemView.findViewById<AppCompatTextView>(R.id.tv_preview)
    fun bind(element: Element) {
        boundedElement = element
        tvTitle.text = element.file.name

        if (element is Document) {
            tvPreview.text = "..."
            GlobalScope.launch(Dispatchers.IO) {
                val full = docContentProvider.getContent(element)
                val preview = if (full.length > 300) {
                    full.substring(0..300)
                } else {
                    full
                }

                withContext(Dispatchers.Main) {
                    if (element == boundedElement) {
                        tvPreview.text = preview
                    }
                }
            }
        } else {
            tvPreview.text = "(folder)"
        }

        itemView.setOnClickListener {
            openDelegate.invoke(element)
        }
    }
}
