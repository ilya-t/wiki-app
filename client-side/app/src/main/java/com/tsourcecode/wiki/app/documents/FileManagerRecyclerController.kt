package com.tsourcecode.wiki.app.documents

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.Closeable

class FileManagerRecyclerController(
        container: ViewGroup,
        private val data: Flow<Folder>,
        openDelegate: (Element) -> Unit,
        docContentProvider: DocumentContentProvider,
) : Closeable {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val context = container.context
    private val progressBar = container.findViewById<View>(R.id.files_trobber)

    private val docAdapter = DocumentsAdapter(docContentProvider).also { adapter ->
        adapter.openDelegate = {
            openDelegate(it)
        }
    }

    private val rv = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = docAdapter

        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    init {
        container.addView(rv)
        scope.launch {
            data.collect {
                progressBar.visibility = View.GONE
                docAdapter.update(it.elements)
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
