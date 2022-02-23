package com.tsourcecode.wiki.app.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
