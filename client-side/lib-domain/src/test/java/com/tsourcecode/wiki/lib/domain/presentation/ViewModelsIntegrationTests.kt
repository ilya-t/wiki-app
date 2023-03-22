package com.tsourcecode.wiki.lib.domain.presentation

import com.tsourcecode.wiki.lib.domain.DomainComponentFactory
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.documents.Folder
import com.tsourcecode.wiki.lib.domain.mocking.Backend
import com.tsourcecode.wiki.lib.domain.mocking.ProjectRevision
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

class ViewModelsIntegrationTests {
    private val v1revision = ProjectRevision(
        message = "revision#1",
        revision = "hash_for_r1",
        rootFileProvider = {
            File("/tmp/README.md").apply {
                writeText("v1")
            }
        }
    )
    private val v2revision = ProjectRevision(
        message = "revision#2",
        revision = "hash_for_r2",
        rootFileProvider = {
            File("/tmp/README.md").apply {
                writeText("v2")
            }
        }
    )

    private val backend = Backend().apply {
        updateRevision(v1revision)
    }

    private val domain = DomainComponentFactory.create(backend.interceptor)

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
        val (_, projectFolder) = waitProjectFolderSynced(v1revision)
        Assert.assertEquals(1, projectFolder.documents.size)
    }

    @Test
    fun `import - sync - view v1`() {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)
        val document = projectFolder.documents.first()

        val viewModel = domain.viewModels.documentViewModelResolver.resolveDocumentViewModel(
            NavigationUtils.openDocument(project, document)
        )
        Assert.assertEquals("v1", viewModel!!.getContent())
        domain.projectComponents.get(project).backendController.sync()
    }

    @Test
    fun `import - sync - view v1 - sync - view v2`() {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)
        val document = projectFolder.documents.first()

        val viewModel = domain.viewModels.documentViewModelResolver.resolveDocumentViewModel(
            NavigationUtils.openDocument(project, document)
        )!!

        val v1Content = viewModel.getContent()

        backend.updateRevision(v2revision)
        viewModel.refresh()
        waitProjectFolderSynced(v2revision)

        val v2Content = viewModel.getContent()
        Assert.assertNotEquals(v2Content, v1Content)
    }

    private fun waitProjectFolderSynced(revision: ProjectRevision): Pair<Project, Folder> {
        val project = domain.projectsRepository.data.value.first()
        val projectFolder = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                domain.projectComponents.get(project).documentsController.data.first {
                    println("Checking: $it")
                    it.revision == revision.revision
                }
            }
        }
        return project to projectFolder.folder
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