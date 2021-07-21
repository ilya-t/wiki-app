package com.tsourcecode.wiki.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.documents.Document
import java.io.File

class DocumentsController(
        container: ViewGroup,
        openDelegate: (Document) -> Unit
) {
    private val context = container.context
    private val docAdapter = DocumentsAdapter(openDelegate)
    private val rv = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = docAdapter
    }

    init {
        docAdapter.update(listOf(
                Document(File("/data/data/some.md")),
                Document(File("/data/data/inbox.md"))
        ))
        container.addView(rv)
    }
}

class DocumentsAdapter(
        private val openDelegate: (Document) -> Unit
) : RecyclerView.Adapter<DocumentViewHolder>() {
    private val items = mutableListOf<Document>()
    fun update(newItems: List<Document>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            DocumentViewHolder(createItemView(parent), openDelegate)

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
        private val openDelegate: (Document) -> Unit
) : RecyclerView.ViewHolder(itemView) {
    private val tvTitle = itemView.findViewById<AppCompatTextView>(R.id.tv_title)
    private val tvPreview = itemView.findViewById<AppCompatTextView>(R.id.tv_preview)
    fun bind(doc: Document) {
        tvTitle.text = doc.file.name
        tvPreview.text = doc.previewText()
        itemView.setOnClickListener {
            openDelegate.invoke(doc)
        }
    }
}
