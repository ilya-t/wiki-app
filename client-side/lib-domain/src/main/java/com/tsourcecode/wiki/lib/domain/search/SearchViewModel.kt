package com.tsourcecode.wiki.lib.domain.search

data class SearchViewModel(
        val searchRequest: String = "",
        val results: List<SearchResultView> = emptyList(),
)
