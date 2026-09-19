package com.tsourcecode.wiki.lib.domain.backend

import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.tests.OpenInTest
import com.tsourcecode.wiki.lib.domain.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local work that could not be rebased and was preserved at remote branches by
 * the server instead of being thrown away.
 */
data class Conflicts(
    val projectName: String,
    val branches: List<String>,
) {
    fun describe(): String {
        val where = if (branches.size == 1) {
            "branch '${branches.first()}'"
        } else {
            "branches ${branches.joinToString(", ") { "'$it'" }}"
        }
        return "CONFLICT: '$projectName' local changes could not be rebased " +
            "and were preserved at remote $where"
    }

    fun comment(): String {
        return if (branches.size == 1) {
            "saved to branch ${branches.first()}"
        } else {
            "saved to ${branches.size} branches: ${branches.joinToString(", ")}"
        }
    }
}

/**
 * Mirrors the conflict branches the backend reports on every status check.
 *
 * The server reads them straight from the remote, so this is self-healing: once
 * the user merges and deletes a branch it simply stops being reported. The last
 * known set is persisted, so a conflict raised by a background sync is still
 * visible right after a restart, before the first status call comes back.
 */
@OpenInTest
class ConflictController(
    storage: KeyValueStorage,
    private val logger: Logger,
) {
    private val storedProject = StoredPrimitive.string(KEY_PROJECT, storage)
    private val storedBranches = StoredPrimitive.stringList(KEY_BRANCHES, storage)

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<Conflicts?> = _state.asStateFlow()

    private fun restore(): Conflicts? {
        val project = storedProject.value ?: return null
        val branches = storedBranches.value.orEmpty()
        if (branches.isEmpty()) {
            return null
        }
        return Conflicts(project, branches)
    }

    /**
     * Applies the full set of branches reported for a project. An empty set
     * clears the conflict, which is how a resolved one disappears.
     */
    fun update(projectName: String, branches: List<String>) {
        val previous = _state.value
        val next = if (branches.isEmpty()) {
            null
        } else {
            Conflicts(projectName, branches.sorted())
        }

        if (next == previous) {
            return
        }

        // A project may only clear its own conflict.
        if (next == null && previous != null && previous.projectName != projectName) {
            return
        }

        next?.let { logger.log { it.describe() } }
        storedProject.value = next?.projectName
        storedBranches.value = next?.branches
        _state.value = next
    }

    fun clear() {
        update(_state.value?.projectName ?: "", emptyList())
    }

    companion object {
        private const val KEY_PROJECT = "last_conflict_project"
        private const val KEY_BRANCHES = "last_conflict_branches"
    }
}
