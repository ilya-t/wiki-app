package com.tsourcecode.wiki.app

import android.app.Activity
import android.os.Bundle
import android.widget.EditText
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
//        BackendController(this)
        setContentView(R.layout.main_layout)
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
        textView.setText(MARKDOWN_SAMPLE.subSequence(0, MARKDOWN_SAMPLE.length - 1))
    }

    companion object {
        private val MARKDOWN_SAMPLE = """
            # Tiny App for quick Android-based experiments

            This tiny app that builds fast and can be used for any kind of __reusable__ prototype or draft.

            Start experiment by

            ```bash
            ./start_experiment.sh my_experiment
            ```
            it will make a checkout to branch with given name that is based on master. Unsaved changes from previous experiment will be automatically commited and now you may do whatever you want!

            ### But wait! Here are couple of fancy tricks!
            Take a look at `com.testspace.CurrentExperiment`, for example:

            ```kotlin
            class CurrentExperiment(a: ExperimentActivity) : Experiment(a) {
                init {
                    a.addTriggers(
                            { a.tvOutput.setText("Trigger#1 pressed") },
                            NamedTrigger("T2") { Static.output("Trigger#2 pressed!") }
                    );
                }

                @LayoutRes
                override fun getExperimentLayout() = R.layout.basic_layout
            }
            ```

            For each trigger a button will be created, this button may be clicked or triggered by keyboard button 1 - to pull first trigger, 2 - to pull second.
            This could be very useful at emulators.

        """.trimIndent()
    }
}