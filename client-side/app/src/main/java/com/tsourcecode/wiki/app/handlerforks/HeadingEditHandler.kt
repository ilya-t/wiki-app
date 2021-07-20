package com.tsourcecode.wiki.app.handlerforks

import android.text.Editable
import android.text.Spanned
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.core.spans.HeadingSpan
import io.noties.markwon.editor.EditHandler
import io.noties.markwon.editor.PersistedSpans

/**
 * fork of https://github.com/noties/Markwon/blob/2ea148c30a07f91ffa37c0aa36af1cf2670441af/app-sample/src/main/java/io/noties/markwon/app/samples/editor/shared/HeadingEditHandler.java
 * just for sample.
 */
class HeadingEditHandler : EditHandler<HeadingSpan> {
    private var theme: MarkwonTheme? = null
    override fun init(markwon: Markwon) {
        theme = markwon.configuration().theme()
    }

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder
                .persistSpan(Head1::class.java) { Head1(theme!!) }
                .persistSpan(Head2::class.java) { Head2(theme!!) }
    }

    override fun handleMarkdownSpan(
            persistedSpans: PersistedSpans,
            editable: Editable,
            input: String,
            span: HeadingSpan,
            spanStart: Int,
            spanTextLength: Int
    ) {
        val type: Class<*>?
        type = when (span.level) {
            1 -> Head1::class.java
            2 -> Head2::class.java
            else -> null
        }
        if (type != null) {
            val index = input.indexOf('\n', spanStart + spanTextLength)
            val end = if (index < 0) input.length else index
            editable.setSpan(
                    persistedSpans[type],
                    spanStart,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun markdownSpanType(): Class<HeadingSpan> {
        return HeadingSpan::class.java
    }

    private class Head1 internal constructor(theme: MarkwonTheme) : HeadingSpan(theme, 1)
    private class Head2 internal constructor(theme: MarkwonTheme) : HeadingSpan(theme, 2)
}
