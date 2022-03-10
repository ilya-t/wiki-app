package com.tsourcecode.wiki.app.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.navigation.ScreenView
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.FileManagerModel
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentProvider
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.net.URI

private const val ROOT_DIR_TITLE = "<root dir>"
private const val SCHEME = "open"


class FileManagerView(
        private val projectComponentProvider: ProjectComponentProvider,
        private val activity: AppCompatActivity,
        docContentProvider: DocumentContentProvider,
        private val projectsRepository: ProjectsRepository,
        private var fileManagerModel: FileManagerModel,
) : ScreenView {
    private val root: View = LayoutInflater.from(activity).inflate(R.layout.file_manager, null)
    override val view: View = root
    private val scope = CoroutineScope(Dispatchers.Main)
    private val container = root.findViewById<ViewGroup>(R.id.files_list_container)
    private val title = root.findViewById<AppCompatTextView>(R.id.files_title).apply {
        setOnClickListener {
            if (text == ROOT_DIR_TITLE) {
                fileManagerModel.notifyRootClicked()
            }
        }
    }
    private val context = container.context
    private val progressBar = root.findViewById<View>(R.id.files_trobber)

    private val docAdapter = DocumentsAdapter(docContentProvider)

    private val rv = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = docAdapter

        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }.also {
        container.addView(it)
    }

    init {
        scope.launch {
            fileManagerModel.dataFlow.filterNotNull().collect {
                progressBar.visibility = View.GONE
                docAdapter.update(it.folder.elements)
                val relativePath = it.folder.file.path.removePrefix(it.project.repo.absolutePath)
                if (relativePath.isNotEmpty()) {
                    title.text = relativePath
                } else {
                    title.text = ROOT_DIR_TITLE
                }
            }
        }
    }

    override fun handle(uri: URI): Boolean {
        return parseNavigation(uri)
    }

    private fun parseNavigation(uri: URI): Boolean {
        if (uri.scheme != SCHEME) {
            return false
        }
        val projectName = uri.host
        val filePath = uri.path.removePrefix("/")

        val p = projectsRepository.data.value.firstOrNull { it.name == projectName } ?: return false

        docAdapter.openDelegate = {
            fileManagerModel.open(p, it)
        }

        fileManagerModel.show(p, filePath)
        return true
    }

    override fun close() {
        scope.cancel()
    }
}
