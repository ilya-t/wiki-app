package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.InMemoryKeyValueStorage
import com.tsourcecode.wiki.lib.domain.util.Logger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import kotlin.test.Test

private const val PROJECT = "notes"

class ConflictControllerTest {
    private val storage = InMemoryKeyValueStorage()
    private val logs = mutableListOf<String>()
    private val logger = Logger(performer = { logs.add(it) })

    private fun controller() = ConflictController(storage, logger)

    @Test
    fun `no conflict by default`() {
        assertNull(controller().state.value)
    }

    @Test
    fun `reported branches become the state`() {
        val controller = controller()
        controller.update(PROJECT, listOf("note_conflict_a3f91c2"))

        assertEquals(
            Conflicts(PROJECT, listOf("note_conflict_a3f91c2")),
            controller.state.value,
        )
    }

    @Test
    fun `an empty report clears a resolved conflict`() {
        val controller = controller()
        controller.update(PROJECT, listOf("note_conflict_a3f91c2"))
        controller.update(PROJECT, emptyList())

        assertNull(controller.state.value)
    }

    @Test
    fun `another project does not clear a conflict`() {
        val controller = controller()
        controller.update(PROJECT, listOf("note_conflict_a3f91c2"))
        controller.update("other", emptyList())

        assertEquals(PROJECT, controller.state.value?.projectName)
    }

    @Test
    fun `conflict survives a restart`() {
        controller().update(PROJECT, listOf("note_conflict_a3f91c2", "note_conflict_0b49dae"))

        // A fresh controller over the same storage: what the app does on launch.
        val restored = controller().state.value
        assertEquals(PROJECT, restored?.projectName)
        assertEquals(
            listOf("note_conflict_0b49dae", "note_conflict_a3f91c2"),
            restored?.branches,
        )
    }

    @Test
    fun `cleared conflict does not come back after a restart`() {
        val controller = controller()
        controller.update(PROJECT, listOf("note_conflict_a3f91c2"))
        controller.clear()

        assertNull(controller().state.value)
    }

    @Test
    fun `conflict is logged once`() {
        val controller = controller()
        controller.update(PROJECT, listOf("note_conflict_a3f91c2"))
        controller.update(PROJECT, listOf("note_conflict_a3f91c2"))

        val conflictLogs = logs.filter { it.contains("note_conflict_a3f91c2") }
        assertEquals(conflictLogs.toString(), 1, conflictLogs.size)
        assertTrue(conflictLogs.first(), conflictLogs.first().startsWith("CONFLICT:"))
    }

    @Test
    fun `comment names a single branch`() {
        assertEquals(
            "saved to branch note_conflict_a3f91c2",
            Conflicts(PROJECT, listOf("note_conflict_a3f91c2")).comment(),
        )
    }

    @Test
    fun `comment counts several branches`() {
        assertEquals(
            "saved to 2 branches: note_conflict_a, note_conflict_b",
            Conflicts(PROJECT, listOf("note_conflict_a", "note_conflict_b")).comment(),
        )
    }
}
