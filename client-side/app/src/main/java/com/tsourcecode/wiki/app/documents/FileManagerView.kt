package com.tsourcecode.wiki.app.documents

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.navigation.ScreenView
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.documents.FileManagerModel
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

private const val ROOT_DIR_TITLE = "<root dir>"

class FileManagerView(
        activity: AppCompatActivity,
        private val componentResolver: ProjectComponentResolver,
        private var fileManagerModel: FileManagerModel,
) : ScreenView {
    private val root: View = LayoutInflater.from(activity).inflate(R.layout.file_manager, null)
    private val swipeRefreshLayout = root.findViewById<SwipeRefreshLayout>(R.id.pull_to_refresh_container)
    override val view: View = root
    private val scope = CoroutineScope(Dispatchers.Main)
    private val container = root.findViewById<ViewGroup>(R.id.files_list_container)
    private val title = root.findViewById<AppCompatTextView>(R.id.files_title).apply {
        setOnClickListener {
            fileManagerModel.notifyRootClicked()
        }
    }
    private val context = container.context
    private val progressBar = root.findViewById<View>(R.id.files_trobber)
    private val docAdapter = DocumentsAdapter()

    private val rv = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = docAdapter

        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }.also {
        container.addView(it)
    }

    override fun handle(uri: URI): Boolean {
        return parseNavigation(uri)
    }

    private fun parseNavigation(uri: URI): Boolean {
        if (!AppNavigator.isFileManagerNavigation(uri)) {
            return false
        }
        val filePath = uri.path.removePrefix("/")

        val component = componentResolver.tryResolve(uri) ?: return false

        docAdapter.openDelegate = {
            fileManagerModel.open(component.project, it)
        }
        docAdapter.docContentProvider = component.docContentProvider

        scope.launch {
            fileManagerModel.show(component, filePath) {
                renderFolder(component.project, it)
            }
        }
        setupPullToRefresh(component)
        return true
    }

    private fun renderFolder(project: Project, folder: Folder) {
        progressBar.visibility = View.GONE
        docAdapter.update(folder.elements)
        val relativePath = folder.file.path.removePrefix(project.repo.absolutePath)
        if (relativePath.isNotEmpty()) {
            title.text = relativePath
        } else {
            title.text = ROOT_DIR_TITLE
        }
    }

    private fun setupPullToRefresh(component: ProjectComponent) {
        swipeRefreshLayout.setOnRefreshListener {
            component.backendController.sync()
        }
        scope.launch {
            component.backendController.refreshFlow.collect { refreshing ->
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = refreshing
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
    }
}
