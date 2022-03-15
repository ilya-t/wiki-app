package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.tests.OpenInTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URI
import java.util.*

@OpenInTest
class AppNavigator {
    private val _data = MutableStateFlow(PROJECTS_URI)
    val data: StateFlow<URI> = _data
    private val stack = Stack<URI>()

    fun open(uri: URI) {
        val existing = _data.value

        if (existing == uri) {
            return
        }

        stack.push(existing)
        _data.value = uri
    }

    fun goBack(): Boolean {
        if (stack.isNotEmpty()) {
            _data.value = stack.pop()
            return true
        }

        return false
    }

    fun clearBackstack() {
        stack.clear()
    }

    companion object {
        val PROJECTS_URI: URI = URI.create("settings://projects")

        fun isFileManagerNavigation(uri: URI) = uri.scheme == "open"
        fun isDocumentEdit(uri: URI) = uri.scheme == "edit"
        fun isChanges(uri: URI) = uri.scheme == "settings" && uri.host == "changes"
        fun isSearch(uri: URI) = uri.scheme == "settings" && uri.host == "search"
    }
}
