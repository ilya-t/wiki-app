package com.tsourcecode.wiki.app

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.tsourcecode.wiki.app.backend.BackendController
import com.tsourcecode.wiki.app.handlerforks.CodeEditHandler
import com.tsourcecode.wiki.app.handlerforks.HeadingEditHandler
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import java.util.concurrent.Executors

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BackendController(this)
        setContentView(R.layout.main_layout)
        configureEditor()
        DocumentsController(
                container = findViewById<ViewGroup>(R.id.files_container),
                openDelegate = {
                    editMd(it.fullText())
                }
        )
        findViewById<View>(R.id.btn_files).setOnClickListener {
            setFilesOpened(true)
        }

        setFilesOpened(true)
//        editMd(MARKDOWN_SAMPLE)
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

    private fun editMd(md: String) {
        setFilesOpened(false)
        val textView = findViewById<EditText>(R.id.tv_markwon)
        textView.setText(md.subSequence(0, md.length - 1))
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