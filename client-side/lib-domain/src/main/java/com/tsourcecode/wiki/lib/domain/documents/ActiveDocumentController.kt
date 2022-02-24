package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.app.documents.Document
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ActiveDocumentController {
    private val _data = MutableStateFlow<Document?>(null)
    val activeDocument: StateFlow<Document?> = _data

    fun switch(d: Document) {
        _data.value = d
    }

    fun close() {
        if (_data.value != null) {
            _data.value = null
        }
    }
}
