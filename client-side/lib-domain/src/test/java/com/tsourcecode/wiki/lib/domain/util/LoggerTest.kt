package com.tsourcecode.wiki.lib.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LoggerTest {
    @Test
    fun composite_forwardsMessagesToAllLoggers() {
        val parentMessages = mutableListOf<String>()
        val sourceMessages = mutableListOf<String>()
        val parent = Logger { parentMessages.add(it) }
        val source = Logger(prefix = "notify: ") { sourceMessages.add(it) }
        val combined = CompositeLogger(parent, source)

        combined.log { "hello" }

        assertEquals(listOf("hello"), parentMessages)
        assertEquals(listOf("notify: hello"), sourceMessages)
    }

    @Test
    fun fork_propagatesSourcesWithForkedPrefix() {
        val sourceMessages = mutableListOf<String>()
        val source = Logger(prefix = "notify: ") { sourceMessages.add(it) }
        val parent = CompositeLogger(Logger { }, source)

        parent.fork("sync: ").log { "progress" }

        assertEquals(listOf("notify: sync: progress"), sourceMessages)
    }

    @Test
    fun composite_forwardsMessagesToBothSources() {
        val source1Messages = mutableListOf<String>()
        val source2Messages = mutableListOf<String>()
        val source1 = Logger { source1Messages.add(it) }
        val source2 = Logger(prefix = "notify: ") { source2Messages.add(it) }
        val combined = CompositeLogger(source1, source2)

        combined.log { "hello" }

        assertEquals(listOf("hello"), source1Messages)
        assertEquals(listOf("notify: hello"), source2Messages)
    }
}
