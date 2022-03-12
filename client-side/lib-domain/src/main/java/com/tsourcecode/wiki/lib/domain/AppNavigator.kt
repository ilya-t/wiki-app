package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URI
import java.util.*

class AppNavigator {
    private val _data = MutableStateFlow(PROJECTS)
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

    companion object {
        val PROJECTS: URI = URI.create("settings://projects")

        fun isFileManagerNavigation(uri: URI) = uri.scheme == "open"
        fun isDocumentEdit(uri: URI) = uri.scheme == "edit"
        fun isChanges(uri: URI) = uri.scheme == "settings" && uri.host == "changes"
        fun isSearch(uri: URI) = uri.scheme == "settings" && uri.host == "search"
    }
}
