package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.presentation.ViewModels
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Playground for debugging purposes. Boots app.
 */
class Playground {
    private val domain = TestDomainComponentFactory.create(
        proxy = TestDomainComponentFactory.ProxyConfig("localhost", 9999)
    )
    val viewModels: ViewModels = domain.viewModels

    fun importProject(): List<ConfigScreenItem> {
        domain.viewModels.configScreenModel.apply {
            submitImport(ConfigScreenItem.ImportFrom())
        }

        val results = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                val items = mutableListOf<List<ConfigScreenItem>>()
                domain.viewModels.configScreenModel.data.take(2).toList(items)
            }
        }
        return results.last()
    }
}

private const val DEFAULT_TIMEOUT = 5_000L
