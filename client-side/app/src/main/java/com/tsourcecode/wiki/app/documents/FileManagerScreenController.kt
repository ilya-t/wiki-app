package com.tsourcecode.wiki.app.documents

import android.app.AlertDialog
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.editor.EditorScreenController
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.app.navigation.ScreenBootstrapper
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

class FileManagerScreenController(
        private val navigator: ActivityNavigator,
        private val documentsController: DocumentsController,
        docContentProvider: DocumentContentProvider,
        private val backendController: BackendController,
        private val editorScreenController: EditorScreenController,
) {
    private val activeFolder = MutableLiveData<Folder>()

    init {
        ScreenBootstrapper(
                Screen.FILE_MANAGER,
                navigator,
                bootPoint = {
                    FileManagerRecyclerController(
                            container = it as ViewGroup,
                            activeFolder,
                            openDelegate = this::openElement,
                            docContentProvider,
                    )
                }
        )
    }

    private fun openElement(e: Element) {
        when (e) {
            is Document -> {
                editorScreenController.data.value = e
                navigator.open(Screen.DOCUMENT)
            }
            is Folder -> navigateTo(e)
        }
    }

    private var currentFolder: Folder? = null
    private val navigationStack = Stack<Folder>()

    init {
        documentsController.data.observeForever {
            if (activeFolder.value == null) {
//                currentFolder = it
                //TODO: update should not happen once-per-session!
                activeFolder.value = it
            }
        }
    }

    fun navigateBackward(): Boolean {
        if (navigationStack.isEmpty()) {
            return false
        }

        navigationStack.pop().let {
            activeFolder.value = it
        }
        return true
    }

    private fun navigateTo(it: Folder) {
        currentFolder?.let {
            navigationStack.push(it)
        }
        activeFolder.value = it
    }

    init {
        documentsController.data.observeForever {
            if (currentFolder == null) {
                currentFolder = it
            }
        }
    }
}
