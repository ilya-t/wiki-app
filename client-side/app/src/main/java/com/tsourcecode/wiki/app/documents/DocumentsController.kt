package com.tsourcecode.wiki.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.documents.DocumentContentProvider
import com.tsourcecode.wiki.app.documents.Element
import com.tsourcecode.wiki.app.documents.Folder
import java.io.File
import java.lang.RuntimeException

class DocumentsController(
        container: ViewGroup,
        openDelegate: (Document) -> Unit,
        private val docContentProvider: DocumentContentProvider,
) {
    fun notifyProjectUpdated(projectDir: String) {
        val dir = File(projectDir)
        if (!dir.isDirectory) {
            throw RuntimeException("Project dir($projectDir) is file!")
        }
        val folder = parseFolder(dir)
        docAdapter.update(folder.documents)
    }

    private fun parseFolder(dir: File): Folder {
        return Folder(
                dir,
                dir.safeListFiles().map { parseElement(it) },
        )
    }

    private fun parseElement(f: File): Element {
        return if (f.isDirectory) {
            parseFolder(f)
        } else {
            Document(f)
        }
    }

    private val context = container.context
    private val docAdapter = DocumentsAdapter(docContentProvider, openDelegate)
    private val rv = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = docAdapter
    }

    init {
        container.addView(rv)
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
    private val tvTitle = itemView.findViewById<AppCompatTextView>(R.id.tv_title)
    private val tvPreview = itemView.findViewById<AppCompatTextView>(R.id.tv_preview)
    fun bind(doc: Document) {
        tvTitle.text = doc.file.name
        tvPreview.text = docContentProvider.getContent(doc)
        itemView.setOnClickListener {
            openDelegate.invoke(doc)
        }
    }
}
