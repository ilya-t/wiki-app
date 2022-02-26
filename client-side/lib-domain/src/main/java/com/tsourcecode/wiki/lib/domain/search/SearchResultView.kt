package com.tsourcecode.wiki.lib.domain.search

import com.tsourcecode.wiki.app.documents.Document

sealed interface SearchResultView

class DocumentSearchResult(
        val document: Document,
) : SearchResultView
