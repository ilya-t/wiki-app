package com.tsourcecode.wiki.lib.domain.presentation

import com.tsourcecode.wiki.lib.domain.TestDomainComponentFactory
import com.tsourcecode.wiki.lib.domain.backend.SyncJob
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewModel
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.documents.Folder
import com.tsourcecode.wiki.lib.domain.mocking.Backend
import com.tsourcecode.wiki.lib.domain.mocking.NOTES_NOT_STAGED_API
import com.tsourcecode.wiki.lib.domain.mocking.NOTES_PROJECT_ROLLBACK_API
import com.tsourcecode.wiki.lib.domain.mocking.NOTES_PROJECT_STAGE_API
import com.tsourcecode.wiki.lib.domain.mocking.ProjectRevision
import com.tsourcecode.wiki.lib.domain.mocking.SYNC_API
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.search.DocumentSearchResult
import com.tsourcecode.wiki.lib.domain.util.NavigationUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_TIMEOUT = 50_000L
private const val SRC_FILE = "main.go"
private const val SRC_FILE_PATH = "src/$SRC_FILE"
private const val README_FILE = "README.md"
private const val LICENCE_FILE = "LICENCE.md"

class ViewModelsIntegrationTests {
    private val v1revision = ProjectRevision(
        message = "revision#1",
        revision = "hash_for_r1",
        rootFileProvider = {
            buildDirectory(
                README_FILE to "v1"
            ).also {
                buildDirectory(
                    File(it, "src"),
                    SRC_FILE to "//Here be a licence.",
                )
            }
        }
    )
    private val v2revision = ProjectRevision(
        message = "revision#2",
        revision = "hash_for_r2",
        rootFileProvider = {
            buildDirectory(
                README_FILE to "v2"
            ).also {
                buildDirectory(
                    File(it, "src"),
                    SRC_FILE to "//Here be a licence.",
                )
            }
        }
    )

    private val backend = Backend().apply {
        updateRevision(v1revision)
    }

    private val domain = TestDomainComponentFactory.create(backend.interceptor)

    @Test
    fun `import`() = runTest {
        val importedResults = importProject()
        val projectElements = importedResults
            .filterIsInstance<ConfigScreenItem.PreviewElement>()

        Assert.assertEquals("Imported results invalid count. expecting: $importedResults " +
                "\ngot: $projectElements", 1, projectElements.size)
        Assert.assertEquals("notes", projectElements.first().projectName)
    }

    @Test
    fun `import - sync`() = runTest {
        importProject()
        val (_, projectFolder) = waitProjectFolderSynced(v1revision)
        Assert.assertEquals("not synced: $projectFolder", 1, projectFolder.documents.size)
    }

    @Test
    fun `import - sync - view v1`() = runTest {
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
    fun `import - view v1 - open search - view recents`() = runTest {
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
    fun `import - edit - sync - see file unchanged`() = runTest {
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
    fun `import - edit - rollback - see file unchanged`() = runTest(timeout = DEFAULT_TIMEOUT.milliseconds) {
        println("=> import")
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)

        println("=> edit")
        val readmeDoc = projectFolder.documents.first { it.file.name == README_FILE }
        backend.updateRevision(v2revision.includeToDiff(readmeDoc))

        println("=> sync again")
        val statusModel = domain.projectComponents.get(project).statusModel
        statusModel.sync().waitWithTimeout()

        println("=> resolving file for rollback")
        val changedFiles: StatusViewItem.FileViewItem = statusModel.statusFlow
            .mapNotNull { viewModel: StatusViewModel ->
                viewModel.items.filterIsInstance<StatusViewItem.FileViewItem>().firstOrNull()
            }
            .first()

        println("=> rollback")
        val v3 = v2revision.copy(revision = "v3")
        backend.updateRevision(v3)
        changedFiles.onRollbackClick()

        statusModel.sync().waitWithTimeout()
        backend.verifyRollbackCall(readmeDoc)
    }

    @Test
    fun `import - edit non-root - rollback`() = runTest(timeout = DEFAULT_TIMEOUT.milliseconds) {
        println("=> import")
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)

        println("=> edit")
        val srcDoc = projectFolder.find(SRC_FILE_PATH) as Document
        backend.updateRevision(v2revision.includeToDiff(srcDoc))

        println("=> sync again")
        val statusModel = domain.projectComponents.get(project).statusModel
        statusModel.sync().waitWithTimeout()

        println("=> resolving file for rollback")
        val changedFiles: StatusViewItem.FileViewItem = statusModel.statusFlow
            .mapNotNull { viewModel: StatusViewModel ->
                viewModel.items.filterIsInstance<StatusViewItem.FileViewItem>().firstOrNull()
            }
            .first()

        println("=> rollback")
        val v3 = v2revision.copy(revision = "v3")
        backend.updateRevision(v3)
        changedFiles.onRollbackClick()

        statusModel.sync().waitWithTimeout()
        backend.verifyRollbackCall(srcDoc)
    }

    @Test
    fun `import - edit non-root - rollback - file in sync after rollback`() =
        runTest(timeout = DEFAULT_TIMEOUT.milliseconds) {
            println("=> import")
            importProject()
            val (project, projectFolder) = waitProjectFolderSynced(v1revision)

            println("=> edit")
            val srcDoc = projectFolder.find(SRC_FILE_PATH) as Document
            backend.updateRevision(v2revision.includeToDiff(srcDoc))

            println("=> sync again")
            val statusModel = domain.projectComponents.get(project).statusModel
            statusModel.sync().waitWithTimeout()

            println("=> resolving file for rollback")
            val changedFiles: StatusViewItem.FileViewItem = statusModel.statusFlow
                .mapNotNull { viewModel: StatusViewModel ->
                    viewModel.items.filterIsInstance<StatusViewItem.FileViewItem>().firstOrNull()
                }
                .first()

            println("=> rollback")
            val v3 = v2revision.copy(revision = "v3")
            backend.updateRevision(v3)
            changedFiles.onRollbackClick()

            statusModel.sync().wait()

            val requestsAfterRollback = backend.captureInterceptions().dropWhile { (req, _) ->
                !req.url.toString().contains(NOTES_PROJECT_ROLLBACK_API)
            }
            val lastSyncApiBody = requestsAfterRollback
                .last { (req, _) ->
                    req.url.toString().endsWith(SYNC_API)
                }
                .first.body?.asString()
            Assert.assertTrue(
                "$SRC_FILE file should be synced with backend after revert! Instead got body: $lastSyncApiBody",
                lastSyncApiBody?.contains(SRC_FILE) == true
            )
        }

    @Test
    fun `import - edit non-root - rollback - old version not staged during sync`() = runTest(timeout = DEFAULT_TIMEOUT.milliseconds) {
        println("=> import")
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)

        println("=> edit")
        val srcDoc = projectFolder.find(SRC_FILE_PATH) as Document
        val locallyChanged = "<File edited locally>"
        srcDoc.file.writeText(locallyChanged)
        backend.updateRevision(v2revision.includeToDiff(srcDoc))

        println("=> sync again")
        val statusModel = domain.projectComponents.get(project).statusModel
        statusModel.sync().waitWithTimeout()

        println("=> resolving file for rollback")
        val changedFiles: StatusViewItem.FileViewItem = statusModel.statusFlow
            .mapNotNull { viewModel: StatusViewModel ->
                viewModel.items.filterIsInstance<StatusViewItem.FileViewItem>().firstOrNull()
            }
            .first()

        println("=> rollback")
        val v3 = v2revision.copy(revision = "v3")
        backend.updateRevision(v3)
        changedFiles.onRollbackClick()

        statusModel.sync().wait()

        val requestsAfterRollback = backend.captureInterceptions().dropWhile { (req, _) ->
            !req.url.toString().contains(NOTES_PROJECT_ROLLBACK_API)
        }

        val noStagedApiRequestBody = requestsAfterRollback.last { (req, _) ->
            req.url.toString().contains(NOTES_NOT_STAGED_API)
        }.first.body?.asString()!!


        Assert.assertFalse("$SRC_FILE should not be considered for staging after rollback: $noStagedApiRequestBody!",
            noStagedApiRequestBody.contains(SRC_FILE))
    }

    @Test
    fun `import - edit - sync - see file staged`() = runTest {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)
        val readmeDoc: Document = projectFolder.documents.first { it.file.name == README_FILE }

        val locallyChanged = "<File edited locally>"
        readmeDoc.file.writeText(locallyChanged)
        backend.updateRevision(v1revision.markUnstaged(readmeDoc))

        domain.projectComponents.get(project).statusModel.sync().waitWithTimeout()
        backend.verifyStageCall(readmeDoc)
    }

    @Test
    fun `import - edit non-root - sync - see file staged`() = runTest {
        importProject()
        val (project, projectFolder) = waitProjectFolderSynced(v1revision)
        val srcDoc: Document = projectFolder.find(SRC_FILE_PATH) as Document

        val locallyChanged = "<File edited locally>"
        srcDoc.file.writeText(locallyChanged)
        backend.updateRevision(v1revision.markUnstaged(srcDoc))

        domain.projectComponents.get(project).statusModel.sync().waitWithTimeout()
        backend.verifyStageCall(srcDoc)
    }

    @Test
    fun `edit - commit - auto-sync to latest revision`() = runTest {
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
        val statusModel: StatusModel = domain.projectComponents.get(project).statusModel
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
        return buildDirectory(root, *nameAndContent)
    }

    private fun buildDirectory(
        root: File,
        vararg nameAndContent: Pair<String, String>
    ): File {
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
                    println("Waiting for local revision '${revision.revision}' got: ${it.revision}")
                    it.revision == revision.revision
                }
                project to projectFolder.folder
            }
        }
    }

    private suspend fun importProject(): List<ConfigScreenItem> {
        domain.viewModels.configScreenModel.submitImport(ConfigScreenItem.ImportFrom()).wait()
        return domain.viewModels.configScreenModel.data.first()
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
        req.url.toString().contains(NOTES_PROJECT_STAGE_API)
    }.map { (req, _) ->
        val buffer = okio.Buffer()
        req.body?.writeTo(buffer)
        val body = buffer.toString()
        RequestResponse(
            url = req.url.toString(),
            response = body
        )
    }
    val calls = stageCalls.joinToString("\n    -")
    Assert.assertNotNull(
        "Document(${readmeDoc.relativePath}) not found at requests to stage api! Stage calls(${stageCalls.size}): ${calls}",
        stageCalls.find { (_, res) -> res.contains(readmeDoc.relativePath) })
}

private fun Backend.verifyRollbackCall(readmeDoc: Document) {
    val capturedCalls: List<RequestResponse> = captureInterceptions().filter { (req, _) ->
        req.url.toString().contains(NOTES_PROJECT_ROLLBACK_API)
    }.map { (req, _) ->
        val buffer = okio.Buffer()
        req.body?.writeTo(buffer)
        val body = buffer.toString()
        RequestResponse(
            url = req.url.toString(),
            response = body
        )
    }
    val prettifiedCalls = capturedCalls.joinToString("\n    -")
    Assert.assertNotNull(
        "Document(${readmeDoc.relativePath}) not found at requests to revert api! Revert calls(${capturedCalls.size}): ${prettifiedCalls}",
        capturedCalls.find { (_, res) -> res.contains(readmeDoc.relativePath) })
}

private data class RequestResponse(
    val url: String,
    val response: String,
)

private fun okhttp3.RequestBody.asString(): String {
    val buffer = okio.Buffer()
    this.writeTo(buffer)
    return buffer.readUtf8()
}

