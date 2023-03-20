package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.DomainComponentFactory
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import org.junit.Test

class DocumentContentProviderTest {
    private val domain = DomainComponentFactory.create()

    @Test
    fun smoke() {
        domain.viewModels.configScreenModel.apply {
            submitImport(ConfigScreenItem.ImportFrom())
        }
    }
}