package com.tsourcecode.wiki.app.editor

import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.app.handlerforks.CodeEditHandler
import com.tsourcecode.wiki.app.handlerforks.HeadingEditHandler
import com.tsourcecode.wiki.app.navigation.ScreenView
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import io.noties.markwon.Markwon
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.Executors

private const val EDITING_ENABLED = false

class EditorScreenView(
        private val appCompatActivity: AppCompatActivity,
        private val projectComponentResolver: ProjectComponentResolver,
) : ScreenView {
    private val root = LayoutInflater.from(appCompatActivity).inflate(
        if (EDITING_ENABLED) R.layout.document_editor else R.layout.document_viewer, null)
    override val view: View = root

    override fun handle(uri: URI): Boolean {
        val results = resolveDocument(uri) ?: return false

        if (EDITING_ENABLED) {
            configureEditor(results.document, results.projectComponent, root)
        } else {
            configureViewer(results.document, results.projectComponent, root)
        }
        return true
    }

    private fun resolveDocument(uri: URI): ResolveResults? {
        if (!AppNavigator.isDocumentEdit(uri)) {
            return null
        }

        val component = projectComponentResolver.tryResolve(uri) ?: return null
        val decodedPath = uri.path.removePrefix("/").split("/").joinToString("/") { URLDecoder.decode(it, "UTF-8") }
        val element = component.documentsController.data.value.find(decodedPath)
        if (element is Document) {
            return ResolveResults(element, component)
        }

        return null
    }

    override fun close() {

    }

    private fun configureViewer(d: Document, component: ProjectComponent, container: View) {
        val textView = container.findViewById<AppCompatTextView>(R.id.tv_markwon)
        val md = component.docContentProvider.getContent(d)
        textView.configureScrolling()

        // parse markdown and create styled text
        val markwon = Markwon.create(textView.context)
        val markdown: Spanned = markwon.toMarkdown(md);
        textView.text = markdown
    }

    private fun configureEditor(d: Document, component: ProjectComponent, container: View) {
        val textView = container.findViewById<AppCompatEditText>(R.id.tv_markwon)
        textView.configureMarkwon()
        textView.configureScrolling()

        val md = component.docContentProvider.getContent(d)
        textView.setText(md.subSequence(0, md.length - 1))
        textView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                component.documentsController.save(d, textView.text.toString())
            }
        })
    }

    private fun AppCompatEditText.configureScrolling() {
        val scroller = Scroller(context)
        setScroller(scroller)
        isVerticalScrollBarEnabled = true
        movementMethod = ScrollingMovementMethod()
        setOnTouchListener(ScrollHandler(scroller, this))
    }

    private fun AppCompatTextView.configureScrolling() {
        val scroller = Scroller(context)
        setScroller(scroller)
        isVerticalScrollBarEnabled = true
        movementMethod = ScrollingMovementMethod()
        setOnTouchListener(ScrollHandler(scroller, this))
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

class ScrollHandler(scroller: Scroller,
                    private val view: TextView) : View.OnTouchListener {
    // Could make this a field member on your activity
    var gesture = GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            scroller.fling(0, view.scrollY, 0, (-velocityY).toInt(), 0, 0, 0, view.lineCount * view.lineHeight)
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
}

private class ResolveResults(
        val document: Document,
        val projectComponent: ProjectComponent,
)