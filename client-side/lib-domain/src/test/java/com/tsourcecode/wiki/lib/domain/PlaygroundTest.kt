package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class PlaygroundTest {
    private val playgroundDir = File("/tmp/wiki_playground").apply {
        mkdirs()
    }
    private val underTest = Playground(
        playgroundDir
    )

    @Test
    @Ignore("only for debugging purposes")
    fun smoke() {
        underTest.importProject()
        val statusScreenModel = underTest.viewModels
            .statusScreenModel("notes")!!
        runBlocking {
            statusScreenModel.sync().wait()
            statusScreenModel.sync().wait()
        }
    }
}