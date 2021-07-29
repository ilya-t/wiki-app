package com.tsourcecode.wiki.app

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.tsourcecode.wiki.app.backend.BackendController
import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.documents.DocumentContentProvider
import com.tsourcecode.wiki.app.handlerforks.CodeEditHandler
import com.tsourcecode.wiki.app.handlerforks.HeadingEditHandler
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val docContentProvider = DocumentContentProvider()
    private lateinit var documentsController: DocumentsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        bootBackend()
        configureEditor()
        findViewById<View>(R.id.btn_files).setOnClickListener {
            setFilesOpened(true)
        }

        setFilesOpened(true)
    }

    private fun bootBackend() {
        val backend = BackendController(
                context = this,
        )

        documentsController = DocumentsController(
                container = findViewById<ViewGroup>(R.id.files_container),
                openDelegate = {
                    editMd(it)
                },
                docContentProvider,
                backend,
        )
    }

    private fun configureEditor() {
        val textView = findViewById<EditText>(R.id.tv_markwon)
        val markwon = Markwon.create(this)
        val editor = MarkwonEditor
                .builder(markwon)
                .useEditHandler(HeadingEditHandler())
                .useEditHandler(EmphasisEditHandler())
                .useEditHandler(StrongEmphasisEditHandler())
                .useEditHandler(CodeEditHandler())
                //                .useEditHandler(StrikethroughEditHandler())
                //                .useEditHandler(BlockQuoteEditHandler())
                //                .useEditHandler(CodeEditHandler())
                .build()
        textView.addTextChangedListener(MarkwonEditorTextWatcher.withPreRender(
                editor, Executors.newSingleThreadExecutor(), textView));
    }

    private fun setFilesOpened(opened: Boolean) {
        if (opened) {
            findViewById<View>(R.id.files_container).visibility = View.VISIBLE
            findViewById<EditText>(R.id.tv_markwon).visibility = View.GONE
            findViewById<View>(R.id.btn_files).visibility = View.GONE
        } else {
            findViewById<View>(R.id.files_container).visibility = View.GONE
            findViewById<EditText>(R.id.tv_markwon).visibility = View.VISIBLE
            findViewById<View>(R.id.btn_files).visibility = View.VISIBLE

        }
    }

    private fun editMd(d: Document) {
        val md = docContentProvider.getContent(d)
        setFilesOpened(false)
        val textView = findViewById<EditText>(R.id.tv_markwon)
        textView.setText(md.subSequence(0, md.length - 1))
        textView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                documentsController.save(d, textView.text.toString())
            }
        })
    }

    override fun onBackPressed() {
        if (areFilesOpened()) {
            super.onBackPressed()
        } else {
            setFilesOpened(true)
        }
    }

    private fun areFilesOpened() = findViewById<View>(R.id.btn_files).visibility != View.VISIBLE
}