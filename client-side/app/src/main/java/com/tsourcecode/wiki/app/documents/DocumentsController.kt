package com.tsourcecode.wiki.lib.domain.documents

import android.app.AlertDialog
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.documents.Element
import com.tsourcecode.wiki.app.documents.Folder
import kotlinx.coroutines.*
import java.io.File
import java.lang.RuntimeException
import java.util.*
import kotlin.Comparator
import android.content.DialogInterface

import android.text.InputType

import android.widget.EditText


class DocumentsController(
        container: ViewGroup,
        openDelegate: (Document) -> Unit,
        private val docContentProvider: DocumentContentProvider,
        private val backendController: BackendController,
) {
    private var currentFolder: Folder? = null
    private val navigationStack = Stack<Folder>()

    init {
        backendController.observeProjectUpdates { notifyProjectUpdated(it) }
    }

    private fun notifyProjectUpdated(projectDir: String) {
        GlobalScope.launch {
            val dir = File(projectDir)
            if (!dir.isDirectory) {
                throw RuntimeException("Project dir($projectDir) is file!")
            }
            val folder = parseFolder(dir, dir)
            currentFolder = folder
            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                docAdapter.update(folder.elements)
            }
        }
    }

    private fun parseFolder(projectDir: File, dir: File): Folder {
        return Folder(
                dir,
                dir.safeListFiles()
                        .map { parseElement(projectDir, it) }
                        .sortedWith(FoldersFirst),
        )
    }


    private object FoldersFirst : Comparator<Element> {
        override fun compare(o1: Element, o2: Element): Int {
            val orderByType = o1.intType().compareTo(o2.intType())

            return if (orderByType == 0) {
                o1.file.name.compareTo(o2.file.name)
            } else {
                orderByType
            }
        }

        private fun Element.intType(): Int {
            return when (this) {
                is Folder -> 0
                is Document -> 1
            }
        }

    }

    private fun parseElement(projectDir: File, f: File): Element {
        return if (f.isDirectory) {
            parseFolder(projectDir, f)
        } else {
            Document(projectDir, f)
        }
    }

    fun save(d: Document, content: String) {
        GlobalScope.launch {
            d.file.writeText(content)
            val b64 = Base64.encodeToString(content.toByteArray(), Base64.DEFAULT)

            backendController.stage(d.relativePath, b64)

            withContext(Dispatchers.Main) {
                btnCommit.visibility = View.VISIBLE
                btnCommit.isEnabled = true
            }
        }
    }

    private fun commit(message: String) {
        backendController.commit(message)
    }

    fun navigateBackward(): Boolean {
        if (navigationStack.isEmpty()) {
            return false
        }

        navigationStack.pop().let {
            currentFolder = it
            docAdapter.update(it.elements)
        }
        return true
    }

    private val context = container.context
    private val progressBar = container.findViewById<View>(R.id.files_trobber)
    private val btnCommit = (container.parent as View).findViewById<View>(R.id.btn_commit).apply {
        setOnClickListener {
            it.isEnabled = false
            showCommitDialog()
        }
    }

    private fun showCommitDialog() {
        val input = EditText(context)
        AlertDialog.Builder(context)
                .setTitle("Enter commit message:")
                .setView(EditText(context))
                .setPositiveButton("OK") { dialog, which ->
                    val commitMessage = input.text.toString()
                    GlobalScope.launch {
                        commit(commitMessage)
                        withContext(Dispatchers.Main) {
                            btnCommit.visibility = View.GONE
                            btnCommit.isEnabled = true
                        }
                    }
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.cancel()
                    btnCommit.visibility = View.VISIBLE
                    btnCommit.isEnabled = true
                }
                .setOnDismissListener {
                    btnCommit.visibility = View.VISIBLE
                    btnCommit.isEnabled = true
                }
                .show()
    }

    private val docAdapter = DocumentsAdapter(docContentProvider).also { adapter ->
        adapter.openDelegate = {
            when (it) {
                is Document -> openDelegate(it)
                is Folder -> {
                    navigateTo(it)
                }
            }
        }
    }

    private fun navigateTo(it: Folder) {
        currentFolder?.let {
            navigationStack.push(it)
        }
        currentFolder = it
        docAdapter.update(it.elements)
    }

    private val rv = RecyclerView(context).apply {
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = docAdapter

        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
    }

    init {
        container.addView(rv, 0)
    }
}

private fun File.safeListFiles(): Array<File> {
    return this.listFiles() ?: emptyArray()
}

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
