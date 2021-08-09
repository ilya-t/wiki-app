package com.tsourcecode.wiki.app.editor

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.lifecycle.MutableLiveData
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.handlerforks.CodeEditHandler
import com.tsourcecode.wiki.app.handlerforks.HeadingEditHandler
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.app.navigation.ScreenBootstrapper
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import java.util.concurrent.Executors

class EditorScreenController(
        navigator: ActivityNavigator,
        private val docContentProvider: DocumentContentProvider,
        private val documentsController: DocumentsController,
) {
    val data = MutableLiveData<Document?>()

    init {
        ScreenBootstrapper(
                Screen.DOCUMENT,
                navigator
        ) {
            data.value?.let { d ->
                configureEditor(d, it)
            }

            null
        }
    }

    private fun configureEditor(d: Document, container: View) {
        val textView = container.findViewById<EditText>(R.id.tv_markwon)
        val markwon = Markwon.create(container.context)
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

        val md = docContentProvider.getContent(d)
        textView.setText(md.subSequence(0, md.length - 1))
        textView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                documentsController.save(d, textView.text.toString())
            }
        })
    }
}