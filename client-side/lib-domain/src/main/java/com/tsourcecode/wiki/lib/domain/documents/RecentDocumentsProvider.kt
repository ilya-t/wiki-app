package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.util.CoroutineScopes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URI

class RecentDocumentsProvider(
    private val navigator: AppNavigator,
    private val scopes: CoroutineScopes,
    private val storedPrimitive: StoredPrimitive<List<String>>,
    private val documentResolver: ProjectDocumentResolver,
) {
    private val navigationsToDocs = MutableStateFlow<List<URI>>(load())

    private fun load(): List<URI> {
        val raw = storedPrimitive.value ?: emptyList()
        return raw.mapNotNull { URI.create(it) }
    }

    val recentDocsFlow: Flow<List<Document>> = navigationsToDocs.map { uris ->
        uris.mapNotNull {
            val document = documentResolver.resolve(it)
            document
        }
    }

    init {
        scopes.worker.launch {
            navigator.data.collect { uri ->
                if (AppNavigator.isDocumentEdit(uri)) {
                    val uris = navigationsToDocs.value + uri
                    navigationsToDocs.value = uris
                    storedPrimitive.value = uris.map { it.toString() }
                }
            }
        }

    }
}