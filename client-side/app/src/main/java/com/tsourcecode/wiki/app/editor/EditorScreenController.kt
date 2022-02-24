package com.tsourcecode.wiki.app.editor

import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Scroller
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.handlerforks.CodeEditHandler
import com.tsourcecode.wiki.app.handlerforks.HeadingEditHandler
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.app.navigation.ScreenBootstrapper
import com.tsourcecode.wiki.lib.domain.documents.ActiveDocumentController
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.DocumentsController
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.Executors


class EditorScreenController(
        private val appCompatActivity: AppCompatActivity,
        private val navigator: ActivityNavigator,
        private val docContentProvider: DocumentContentProvider,
        private val documentsController: DocumentsController,
        private val activeDocumentController: ActiveDocumentController,
) {
    init {
        openDocAutomatically()

        ScreenBootstrapper(
                Screen.DOCUMENT,
                navigator
        ) {
            activeDocumentController.activeDocument.value?.let { d ->
                configureEditor(d, it)
                appCompatActivity.title = d.relativePath
            }

            null
        }


    }

    //TODO: remove this navigation crutch
    private fun openDocAutomatically() {
        appCompatActivity.lifecycleScope.launch {
            activeDocumentController.activeDocument.collect {
                if (it != null) {
                    navigator.open(Screen.DOCUMENT)
                }
            }
        }

        navigator.data.observe(appCompatActivity) {
            if (it.screen != Screen.DOCUMENT) {
                activeDocumentController.close()
            }
        }
    }

    private fun configureEditor(d: Document, container: View) {
        val textView = container.findViewById<AppCompatEditText>(R.id.tv_markwon)
        textView.configureMarkwon()
        textView.configureScrolling()

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

    private fun AppCompatEditText.configureScrolling() {
        val scroller = Scroller(context)
        setScroller(scroller)
        isVerticalScrollBarEnabled = true
        movementMethod = ScrollingMovementMethod()
        setOnTouchListener(object : OnTouchListener {
            // Could make this a field member on your activity
            var gesture = GestureDetector(context, object : SimpleOnGestureListener() {
                override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    scroller.fling(0, scrollY, 0, (-velocityY).toInt(), 0, 0, 0, lineCount * lineHeight)
                    return super.onFling(e1, e2, velocityX, velocityY)
                }

                override fun onDown(e: MotionEvent?): Boolean {
                    scroller.abortAnimation()
                    return false
                }
            })

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                gesture.onTouchEvent(event)
                return false
            }
        })
    }

    private fun AppCompatEditText.configureMarkwon() {
        val markwon = Markwon.create(context)
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
        addTextChangedListener(MarkwonEditorTextWatcher.withPreRender(
                editor, Executors.newSingleThreadExecutor(), this));
    }
}