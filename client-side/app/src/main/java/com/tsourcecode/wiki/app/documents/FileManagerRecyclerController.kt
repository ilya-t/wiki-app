package com.tsourcecode.wiki.app.documents

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.Closeable

private const val ROOT_DIR_TITLE = "<root dir>"

class FileManagerRecyclerController(
        private val project: Project,
        private val activity: AppCompatActivity,
        root: ViewGroup,
        private val data: Flow<Folder>,
        openDelegate: (Element) -> Unit,
        rootClickDelegate: () -> Unit,
        docContentProvider: DocumentContentProvider,
) : Closeable {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val container = root.findViewById<ViewGroup>(R.id.files_list_container)
    private val title = root.findViewById<AppCompatTextView>(R.id.files_title).apply {
        setOnClickListener {
            if (text == ROOT_DIR_TITLE) {
                rootClickDelegate()
            }
        }
    }
    private val context = container.context
    private val progressBar = root.findViewById<View>(R.id.files_trobber)

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
                val relativePath = it.file.path.removePrefix(project.repo.absolutePath)
                if (relativePath.isNotEmpty()) {
                    title.text = relativePath
                } else {
                    title.text = ROOT_DIR_TITLE
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
