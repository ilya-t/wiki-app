package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Ignore
import org.junit.Test

class PlaygroundTest {
    private val underTest = Playground()

    @Test
    @Ignore("only for debugging purposes")
    fun smoke() {
        underTest.importProject()
        val statusScreenModel = underTest.viewModels
            .statusScreenModel("notes")!!
        runBlocking {
            withTimeout(5000L) {
                statusScreenModel.sync().wait()
            }
            withTimeout(5000L) {
                statusScreenModel.sync().wait()
            }
        }
    }
}