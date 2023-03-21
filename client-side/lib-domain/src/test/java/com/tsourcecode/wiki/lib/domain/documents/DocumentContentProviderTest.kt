package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.lib.domain.DomainComponentFactory
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.mocking.ApiResponseInterceptor
import com.tsourcecode.wiki.lib.domain.mocking.ResponseMaker
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test

class DocumentContentProviderTest {
    private val domain = DomainComponentFactory.create(
        ApiResponseInterceptor(mapOf(
            "/api/1/projects" to ResponseMaker.JsonResponse(
                """
                    {
                        "configs": [
                            {
                                "name": "notes",
                                "repo_url": "git@github.com:username/notes.git"
                            }
                        ]
                    }
                """.trimIndent()
            ),
        ))
    )

    @Test
    fun smoke() {
        domain.viewModels.configScreenModel.apply {
            submitImport(ConfigScreenItem.ImportFrom("http://localhost"))
        }


        val results = runBlocking {
            withTimeout(5_000L) {
                val items = mutableListOf<List<ConfigScreenItem>>()
                domain.viewModels.configScreenModel.data.take(2).toList(items)
            }
        }


        Assert.assertEquals(1, results.last().filterIsInstance<ConfigScreenItem.PreviewElement>().size)
    }
}