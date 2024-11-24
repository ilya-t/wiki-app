package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.tests.OpenInTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.URI
import java.net.URLDecoder
import java.util.Stack

@OpenInTest
class AppNavigator(
    private val lastUri: StoredPrimitive<String>,
) {
    private val _data = MutableStateFlow(PROJECTS_URI)
    val data: StateFlow<URI> = _data
    private val stack = Stack<URI>()


    init {
        lastUri.value?.let { URI.create(it) }?.let {
            open(it)
        }
    }

    fun open(uri: URI) {
        val existing = _data.value

        if (existing == uri) {
            return
        }

        stack.push(existing)
        _data.value = uri
        lastUri.value = uri.toString()
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

        fun extractDocumentPath(uri: URI): String? {
            if (!isDocumentEdit(uri)) {
                return null
            }

            return uri.path
                .removePrefix("/")
                .split("/")
                .joinToString("/") { URLDecoder.decode(it, "UTF-8") }
        }
    }
}
