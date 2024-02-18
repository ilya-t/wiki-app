package com.tsourcecode.wiki.lib.domain.presentation

import com.tsourcecode.wiki.lib.domain.DomainComponentFactory
import com.tsourcecode.wiki.lib.domain.backend.SyncJob
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.documents.Folder
import com.tsourcecode.wiki.lib.domain.mocking.Backend
import com.tsourcecode.wiki.lib.domain.mocking.NOTES_PROJECT_STAGE_API
import com.tsourcecode.wiki.lib.domain.mocking.ProjectRevision
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.search.DocumentSearchResult
import com.tsourcecode.wiki.lib.domain.util.NavigationUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.UUID

private const val DEFAULT_TIMEOUT = 5_000L
private const val README_FILE = "README.md"
private const val LICENCE_FILE = "LICENCE.md"

class ViewModelsIntegrationTests {
    private val v1revision = ProjectRevision(
        message = "revision#1",
        revision = "hash_for_r1",
        rootFileProvider = {
            buildDirectory(
                README_FILE to "v1"
            )
        }
    )
    private val v2revision = ProjectRevision(
        message = "revision#2",
        revision = "hash_for_r2",
        rootFileProvider = {
            buildDirectory(
                README_FILE to "v2"
            )
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

        Assert.assertEquals("Instead import contains: $importedResults", 1, projectElements.size)
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
    fun `import - sync - view v1 - sync - view v1(local content wins)`() {
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

        val contentAfterSync = viewModel.getContent()
        Assert.assertEquals(v1Content, contentAfterSync)
    }

    @Test
    fun `import - view v1 - open search - view recents`() {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)
        val document = projectFolder.documents.first()
        domain.navigator.open(NavigationUtils.openDocument(project, document))

        val searchModel = domain.viewModels.searchScreenModel(domain.projectComponents.get(project))
        val searchViewModel = runBlocking {
            withTimeout(DEFAULT_TIMEOUT) {
                searchModel.data.first {
                    it.results.isNotEmpty()
                }
            }
        }

        val result = searchViewModel.results.get(0) as DocumentSearchResult
        Assert.assertEquals(document, result.document)
    }

    @Test
    fun `import - edit - sync - see file unchanged`() {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)
        val readmeDoc = projectFolder.documents.first { it.file.name == README_FILE }

        val locallyChanged = "<File edited locally>"
        readmeDoc.file.writeText(locallyChanged)
        domain.projectComponents.get(project).statusModel.sync()
        waitProjectFolderSynced(v1revision)

        Assert.assertEquals(locallyChanged, readmeDoc.file.readText())
    }

    @Test
    fun `import - edit - sync - see file staged`() {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)
        val readmeDoc = projectFolder.documents.first { it.file.name == README_FILE }

        val locallyChanged = "<File edited locally>"
        readmeDoc.file.writeText(locallyChanged)

        domain.projectComponents.get(project).statusModel.sync().waitWithTimeout()
        backend.verifyStageCall(readmeDoc)
    }

    @Test
    fun `edit - commit - auto-sync to latest revision`() {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)
        val readmeDoc = projectFolder.documents.first { it.file.name == README_FILE }

        val locallyChanged = "<File edited locally>"
        readmeDoc.file.writeText(locallyChanged)

        val next = ProjectRevision(
            message = "v3",
            rootFileProvider = {
                buildDirectory(
                    README_FILE to locallyChanged,
                    LICENCE_FILE to "apache 2.0",
                )
            }
        )
        backend.updateRevision(next)
        val statusModel = domain.projectComponents.get(project).statusModel
        statusModel.updateCommitText("upd")
        statusModel.commit()
        waitProjectFolderSynced(next)

        val changes = statusModel
            .statusFlow
            .value
            .items
            .filterIsInstance<StatusViewItem.FileViewItem>()
        Assert.assertEquals(
            "Got Changes: ",
            0, changes.size)
    }

    private fun buildDirectory(vararg nameAndContent: Pair<String, String>): File {
        val root = File("/tmp/${UUID.randomUUID()}")
        root.mkdirs()
        nameAndContent.forEach { (name, content) ->
            File(root, name).writeText(content)
        }
        return root
    }

    private fun waitProjectFolderSynced(revision: ProjectRevision): Pair<Project, Folder> {
        return runBlocking {
            withTimeout(5000L) {
                val projects = domain.projectsRepository.data.first { it.isNotEmpty() }
                val project = projects.first()
                val projectComponent = domain.projectComponents.get(project)
                val projectFolder = projectComponent.documentsController.data.first {
                    println("want: ${revision.revision} got: ${it.revision}")
                    it.revision == revision.revision
                }
                project to projectFolder.folder
            }
        }
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

private fun SyncJob.waitWithTimeout() {
    runBlocking {
        withTimeout(DEFAULT_TIMEOUT) {
            wait()
        }
    }
}

private fun Backend.verifyStageCall(readmeDoc: Document) {
    val stageCalls: List<RequestResponse> = captureInterceptions().filter { (req, _) ->
        req.url().toString().contains(NOTES_PROJECT_STAGE_API)
    }.map { (req, _) ->
        val buffer = okio.Buffer()
        req.body()?.writeTo(buffer)
        val body = buffer.toString()
        RequestResponse(
            url = req.url().toString(),
            response = body
        )
    }
    val calls = stageCalls.joinToString("\n    -")
    Assert.assertNotNull(
        "Document(${readmeDoc.relativePath}) not found at requests to stage api! Stage calls(${stageCalls.size}): ${calls}",
        stageCalls.find { (_, res) -> res.contains(readmeDoc.relativePath) })
}

private data class RequestResponse(
    val url: String,
    val response: String,
)
