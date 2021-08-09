package com.tsourcecode.wiki.app.documents

import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.DocumentsAdapter
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.*

class FileManagerRecyclerController(
        container: ViewGroup,
        private val data: LiveData<Folder>,
        openDelegate: (Element) -> Unit,
        docContentProvider: DocumentContentProvider,
) : Closeable {
    private val dataObserver = Observer<Folder> {
        progressBar.visibility = View.GONE
        docAdapter.update(it.elements)
    }
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
        data.observeForever(dataObserver)
    }

    override fun close() {
        data.removeObserver(dataObserver)
    }
}
