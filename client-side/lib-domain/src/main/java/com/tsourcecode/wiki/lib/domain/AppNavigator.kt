package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.net.URI
import java.util.*

private const val SCHEME = "open"

class AppNavigator {
    private val _data = MutableStateFlow(PROJECTS)
    val data: Flow<URI> = _data
    private val stack = Stack<URI>()

    fun open(uri: URI) {
        val existing = _data.value

        if (existing == uri) {
            return
        }

        stack.push(existing)
        _data.value = uri
    }

    fun goBack() {
        if (stack.isNotEmpty()) {
            _data.value = stack.pop()
        }
    }

    companion object {
        val PROJECTS: URI = URI.create("$SCHEME://projects")
    }
}
