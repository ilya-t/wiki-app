package com.tsourcecode.wiki.app.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.Element
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DocumentsAdapter() : RecyclerView.Adapter<DocumentViewHolder>() {
    var docContentProvider: DocumentContentProvider? = null
    var openDelegate: (Element) -> Unit = {}
    private val items = mutableListOf<Element>()
    fun update(newItems: List<Element>) {
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
        holder.bind(items[position], docContentProvider!!)
    }

    override fun getItemCount() = items.size
}

class DocumentViewHolder(
    itemView: View,
    private val openDelegate: (Element) -> Unit,
) : RecyclerView.ViewHolder(itemView) {
    private var boundedElement: Element? = null
    private val tvTitle = itemView.findViewById<AppCompatTextView>(R.id.tv_title)
    private val tvPreview = itemView.findViewById<AppCompatTextView>(R.id.tv_preview)
    private var scope = CoroutineScope(Dispatchers.IO)
    fun bind(element: Element,
             docContentProvider: DocumentContentProvider,
    ) {
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO)
        boundedElement = element
        tvTitle.text = element.file.name

        if (element is Document) {
            tvPreview.text = "..."
            scope.launch(Dispatchers.IO) {
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
