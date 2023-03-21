package com.tsourcecode.wiki.lib.domain.presentation

import com.tsourcecode.wiki.app.documents.Folder
import com.tsourcecode.wiki.lib.domain.DomainComponentFactory
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.mocking.ApiResponseInterceptor
import com.tsourcecode.wiki.lib.domain.mocking.Archiver
import com.tsourcecode.wiki.lib.domain.mocking.ResponseMaker
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.util.NavigationUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test
import java.io.File

private const val DEFAULT_TIMEOUT = 5_000L
private const val REVISION_ZIP_REPO_DIR = "repo"

class ViewModelsIntegrationTests {
    private val responseInterceptor = ApiResponseInterceptor(mapOf(
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
        "/api/1/status" to ResponseMaker.JsonResponse(
            """
                    {"files":[]}
                """.trimIndent()
        ),
        "/api/1/revision/latest" to ResponseMaker.FileResponse(
            file = File("/tmp/repository.zip"),
            setup = { file ->
                Archiver.zipFile(
                    src = File("/tmp/README.md").apply {
                        writeText("v1")
                    },
                    dst = file,
                    rootDir = REVISION_ZIP_REPO_DIR,
                )
            }
        ),
        "/notes/api/1/revision/show" to ResponseMaker.JsonResponse(
            """
                    {
                        "revision": "HEAD",
                        "message": "Initial commit",
                        "date": "Today"
                    }
                """.trimIndent()
        )
    ))
    private val domain = DomainComponentFactory.create(
        responseInterceptor
    )

    @Test
    fun `import`() {
        val importedResults = importProject()
        val projectElements = importedResults
            .filterIsInstance<ConfigScreenItem.PreviewElement>()

        Assert.assertEquals(1, projectElements.size)
        Assert.assertEquals("notes", projectElements.first().projectName)
    }

    @Test
    fun `import - sync`() {
        importProject()
        val (_, projectFolder) = waitProjectFolderSynced()
        Assert.assertEquals(1, projectFolder.documents.size)
    }

    @Test
    fun `import - sync - view v1`() {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced()
        val document = projectFolder.documents.first()

        val viewModel = domain.viewModels.documentViewModelResolver.resolveDocumentViewModel(
            NavigationUtils.openDocument(project, document)
        )
        Assert.assertEquals("v1", viewModel!!.getContent())
    }

    private fun waitProjectFolderSynced(): Pair<Project, Folder> {
        val project = domain.projectsRepository.data.value.first()
        val folder = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                val data = domain.projectComponents.get(project).documentsController.data
                data.first { it.documents.isNotEmpty() }
            }
        }
        return project to folder
    }

    private fun importProject(): List<ConfigScreenItem> {
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