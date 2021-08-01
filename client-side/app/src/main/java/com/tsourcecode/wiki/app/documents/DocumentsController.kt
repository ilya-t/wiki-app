package com.tsourcecode.wiki.lib.domain.documents

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.documents.Element
import com.tsourcecode.wiki.app.documents.Folder
import kotlinx.coroutines.*
import java.io.File
import java.lang.RuntimeException

class DocumentsController(
        container: ViewGroup,
        openDelegate: (Document) -> Unit,
        private val docContentProvider: DocumentContentProvider,
        private val backendController: BackendController,
) {
    init {
        backendController.observeProjectUpdates { notifyProjectUpdated(it) }
    }

    private fun notifyProjectUpdated(projectDir: String) {
        GlobalScope.launch {
            val dir = File(projectDir)
            if (!dir.isDirectory) {
                throw RuntimeException("Project dir($projectDir) is file!")
            }
            val folder = parseFolder(dir, dir)
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                docAdapter.update(folder.documents)
            }
        }
    }

    private fun parseFolder(projectDir: File, dir: File): Folder {
        return Folder(
                dir,
                dir.safeListFiles().map { parseElement(projectDir, it) },
        )
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
            d.file.writeText(content)
            val b64 = Base64.encodeToString(content.toByteArray(), Base64.DEFAULT)

            backendController.stage(d.relativePath, b64)

            withContext(Dispatchers.Main) {
                btnCommit.visibility = View.VISIBLE
                btnCommit.isEnabled = true
            }
        }
    }

    private fun commit() {
        backendController.commit()
    }

    private val context = container.context
    private val progressBar = container.findViewById<View>(R.id.files_trobber)
    private val btnCommit = (container.parent as View).findViewById<View>(R.id.btn_commit).apply {
        setOnClickListener {
            it.isEnabled = false
            GlobalScope.launch {
                commit()
                withContext(Dispatchers.Main) {
                    visibility = View.GONE
                    it.isEnabled = true
                }
            }
        }
    }
    private val docAdapter = DocumentsAdapter(docContentProvider, openDelegate)
    private val rv = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = docAdapter

        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    init {
        container.addView(rv, 0)
    }
}

private fun File.safeListFiles(): Array<File> {
    return this.listFiles()?: emptyArray()
}

class DocumentsAdapter(
        private val docContentProvider: DocumentContentProvider,
        private val openDelegate: (Document) -> Unit
) : RecyclerView.Adapter<DocumentViewHolder>() {
    private val items = mutableListOf<Document>()
    fun update(newItems: List<Document>) {
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
        private val openDelegate: (Document) -> Unit,
        private val docContentProvider: DocumentContentProvider
) : RecyclerView.ViewHolder(itemView) {
    private var boundedDoc: Document? = null
    private val tvTitle = itemView.findViewById<AppCompatTextView>(R.id.tv_title)
    private val tvPreview = itemView.findViewById<AppCompatTextView>(R.id.tv_preview)
    fun bind(doc: Document) {
        boundedDoc = doc
        tvTitle.text = doc.file.name
        tvPreview.text = "..."
        GlobalScope.launch(Dispatchers.IO) {
            val full = docContentProvider.getContent(doc)
            val preview = if (full.length > 300) {
                full.substring(0..300)
            } else {
                full
            }

            withContext(Dispatchers.Main) {
                if (doc == boundedDoc) {
                    tvPreview.text = preview
                }
            }
        }

        itemView.setOnClickListener {
            openDelegate.invoke(doc)
        }
    }
}
