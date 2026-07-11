package com.tsourcecode.wiki.lib.domain.integration_tests

import com.tsourcecode.wiki.lib.domain.DomainComponent
import com.tsourcecode.wiki.lib.domain.JdkPlatformDeps
import com.tsourcecode.wiki.lib.domain.TestDomainComponentFactory
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private const val TEST_PROJECT = "test_repo"

/**
 * Heavy integrational test that will boot local server container against which sync will be checked.
 */
class SyncTests {
    private val timeout: Duration = 1.minutes

    private fun integrationTest(block: suspend () -> Unit) = runBlocking {
        withTimeout(timeout) {
            block()
        }
    }
    // CI_TEST_ENV_DIR: host-visible path for nested Docker volume mounts in CI (see testrun.sh).
    private val testDir = (
        System.getenv("CI_TEST_ENV_DIR")?.let { File(it, "syncTests_${UUID.randomUUID()}") }
            ?: File("/tmp/syncTests_${UUID.randomUUID()}")
        ).also {
        it.mkdirs()
    }
    private val clientFiles = File(testDir, "client-side")
    private val domain = TestDomainComponentFactory.create(filesRoot = clientFiles)
    private val serverFiles = File(testDir, "server-side")

    @get:Rule val rule = TestName()

    private lateinit var serverController: ServerController

    init {
        println("Test artifacts are available at: '$testDir'")
    }

    @Before
    fun setUp() {
        ServerController.ensureStopped(locateServerSideDir())
        serverController = ServerController(
            serverSideDir = locateServerSideDir(),
            serverFiles = serverFiles,
            alias = rule.methodName
        ).also {
            it.start()
            it.waitHeartbeats()
        }
    }

    @After
    fun tearDown() {
        if (::serverController.isInitialized) {
            serverController.stop()
        }
        println("Test artifacts are available at: $testDir")
    }

    @Test
    fun `server has test repo`() = integrationTest {
        val projects: List<ConfigScreenItem> = importProjects()
        val previewElement: ConfigScreenItem.PreviewElement = projects
            .filterIsInstance<ConfigScreenItem.PreviewElement>()
            .first()


        Assert.assertEquals(TEST_PROJECT, previewElement.projectName)
    }

    @Test
    fun `initial sync with repo from server`() = integrationTest {
        val statusModel: StatusModel = openFirstProjectStatus()
        statusModel.sync("testing").wait()

        val initialFile = File(captureTestProject().dir, "README.md")
        Assert.assertTrue(initialFile.exists())
    }

    @Test
    fun `once file edited it is shown at status`() = integrationTest {
        val statusModel: StatusModel = openFirstProjectStatus()
        println("--> Waiting for sync")
        statusModel.sync("waiting initial sync completes").wait()

        println("--> Updating local file")
        val initialFile = File(captureTestProject().dir, "README.md")
        initialFile.writeText("<README.md updated content>")

        println("--> Making another sync")
        statusModel.sync("sync after local changes").wait()

        println("--> Waiting to see changed files")
        val files: List<StatusViewItem.FileViewItem> = statusModel.statusFlow
            .map { it.items.filterIsInstance<StatusViewItem.FileViewItem>() }
            .first { it.isNotEmpty() }

        Assert.assertEquals(1, files.size)
        files.first().fileStatus.path
            .let { path ->
                Assert.assertEquals("README.md", path)
            }
    }

    @Test
    fun `no changes after first sync`() = integrationTest {
        val statusModel: StatusModel = openFirstProjectStatus()
        statusModel.sync("testing").wait()

        val items: List<StatusViewItem> = statusModel.statusFlow.first {
            it.items.isNotEmpty()
        }.items

        Assert.assertEquals("No changed files expected. Instead got: $items",
            0, items.filterIsInstance<StatusViewItem.FileViewItem>().size)
        Assert.assertEquals("Revision item should be present. Instead got: $items",
            1, items.filterIsInstance<StatusViewItem.RevisionViewItem>().size)
    }

    @Test
    fun `add new file`() = integrationTest {
        val statusModel: StatusModel = openFirstProjectStatus()
        statusModel.sync("testing").wait()

        val newFile = File(captureTestProject().dir, "new.md")
        println("===> Adding new local file: ${newFile.absolutePath}")
        newFile.writeText("<new.md content>")

        println("===> Making another sync")
        statusModel.sync("testing").wait()

        println("===> Waiting for diff to appear at status")
        val files: List<StatusViewItem.FileViewItem> = statusModel.statusFlow
            .map { it.items.filterIsInstance<StatusViewItem.FileViewItem>() }
            .first { it.isNotEmpty() }

        Assert.assertEquals("Expecting only 1 changed file. Instead got: ${files.map { it.fileStatus }}",
            1, files.size)
        files.first().fileStatus.path
            .let { path ->
                Assert.assertEquals("${files.map { it.fileStatus }}", newFile.name, path)
            }
    }

    @Test
    fun `pull changes from server`() = integrationTest {
        val statusModel: StatusModel = openFirstProjectStatus()
        statusModel.sync("testing").wait()

        val repo = File(serverFiles, "test_repo")
        val readme = File(repo, "README.md")
        println("===> Updating local file: ${readme.absolutePath}")
        val changes = "Nearly-commited line"
        readme.appendText(changes)

        val commitMessage = "pull changes from server"
        exec(
            cmd = "git add README.md && git commit -m '$commitMessage' && git push origin master:master",
            cwd = repo
        )

        println("===> Making another sync")
        statusModel.sync("testing").waitResults().exceptionOrNull()?.let {
            throw AssertionError(it)
        }

        println("===> Waiting for diff to appear at status")
        statusModel.statusFlow
            .map {
                println("Status: $it")
                it.items.filterIsInstance<StatusViewItem.RevisionViewItem>()
            }
            .first { items: List<StatusViewItem.RevisionViewItem> ->
                items.any { it.message.contains(commitMessage) }
            }

        val files: List<StatusViewItem.FileViewItem> = statusModel
            .statusFlow
            .value
            .items
            .filterIsInstance<StatusViewItem.FileViewItem>()

        Assert.assertEquals("Expecting no changed file. Instead got: ${files.map { it.fileStatus }}",
            0, files.size)


        val localReadMe = File(captureTestProject().dir, "README.md")
        val body = localReadMe.readLines().joinToString("\n")
        Assert.assertTrue("Body not contains '$changes'. instead got: '$body'", body.contains(changes))
    }

    @Test
    fun `add new file and roll it back`() = integrationTest {
        val statusModel: StatusModel = openFirstProjectStatus()
        statusModel.sync("testing").wait()

        val newFile = File(captureTestProject().dir, "new.md")
        println("===> Adding new local file: ${newFile.absolutePath}")
        newFile.writeText("<new.md content>")

        println("===> Making another sync")
        statusModel.sync("testing").wait()

        println("===> Waiting for diff to appear at status")
        val files: List<StatusViewItem.FileViewItem> = statusModel.statusFlow
            .map { it.items.filterIsInstance<StatusViewItem.FileViewItem>() }
            .first { it.isNotEmpty() }


        println("===> Rolling back changes")
        files.first { it.fileStatus.path.endsWith(newFile.name) }
            .onRollbackClick
            .invoke()
            .wait()

        println("===> Making another sync after roll back")
        statusModel.sync("testing").wait()


        Assert.assertFalse(newFile.exists())
    }

    @Test
    fun `recovers cleanly after interrupted multi-file pull`() = integrationTest {
        val testClientFiles = File(testDir, "client-side-interrupted")
        val responseInterceptor = NetworkInterceptorByFileExistence()
        val testDomain = TestDomainComponentFactory.create(
            responseInterceptor = responseInterceptor,
            filesRoot = testClientFiles,
            throwOnQuickStatusError = false,
        )

        val statusModel = openFirstProjectStatus(testDomain)
        statusModel.sync("initial sync").wait()

        val remoteFiles = listOf("remote-a.md", "remote-b.md", "remote-c.md")
        val commitMessage = commitNewFilesAtServerSide(remoteFiles)

        val projectDir = captureTestProject(testDomain).dir
        responseInterceptor.projectDir = projectDir
        responseInterceptor.interruptStatusAfterOnFilesExists(remoteFiles)

        statusModel.sync("interrupted pull").wait()

        val revisionAfterInterrupt = statusModel.statusFlow.value.items
            .filterIsInstance<StatusViewItem.RevisionViewItem>()
            .firstOrNull()
            ?.message
        Assert.assertFalse(
            "Revision should not advance after interrupted sync, got: $revisionAfterInterrupt",
            revisionAfterInterrupt?.contains(commitMessage) == true,
        )

        responseInterceptor.interruptStatusAfterOnFilesExists(emptyList())
        statusModel.sync("recovery pull").waitResults().exceptionOrNull()?.let {
            throw AssertionError(it)
        }

        val items = statusModel.statusFlow.first { it.items.isNotEmpty() }.items
        Assert.assertEquals(
            "No changed files expected. Instead got: $items",
            0, items.filterIsInstance<StatusViewItem.FileViewItem>().size,
        )
        Assert.assertEquals(
            "Revision item should be present. Instead got: $items",
            1, items.filterIsInstance<StatusViewItem.RevisionViewItem>().size,
        )

        remoteFiles.forEach { name ->
            val file = File(projectDir, name)
            Assert.assertTrue("Expected $name to exist locally", file.exists())
            Assert.assertEquals("content of $name", file.readText())
        }
    }

    private suspend fun openFirstProjectStatus(
        domain: DomainComponent<JdkPlatformDeps> = this.domain,
    ): StatusModel {
        val projects: List<ConfigScreenItem> = importProjects(domain)
        val previewElement: ConfigScreenItem.PreviewElement = projects
            .filterIsInstance<ConfigScreenItem.PreviewElement>()
            .first()
        val statusModel = domain.viewModels.statusScreenModel(previewElement.projectName)
            ?: throw IllegalStateException("Status screen not found for ${previewElement.projectName} ")
        // BackendController init schedules pullOrSync("project init") asynchronously.
        // Drain the queue so later edits are not overwritten by a stale full sync.
        statusModel.sync("drain project init").wait()
        statusModel.sync("drain project init").wait()
        return statusModel
    }

    private suspend fun importProjects(
        domain: DomainComponent<JdkPlatformDeps> = this.domain,
    ): List<ConfigScreenItem> {
        domain.viewModels.configScreenModel.submitImport(
            ConfigScreenItem.ImportFrom(
                projectUrl = serverController.serverUrl
            )
        ).wait()
        return domain.viewModels.configScreenModel.data.first {
            it.filterIsInstance<ConfigScreenItem.PreviewElement>().isNotEmpty()
        }
    }

    private suspend fun captureTestProject(
        domain: DomainComponent<JdkPlatformDeps> = this.domain,
    ): Project {
        val projects = domain.projectsRepository.data.first { it.isNotEmpty() }
        return projects.first { it.name == TEST_PROJECT }
    }

    private fun commitNewFilesAtServerSide(
        remoteFiles: List<String>,
        commitMessage: String = "add remote files",
    ): String {
        val repo = File(serverFiles, "test_repo")
        remoteFiles.forEach { name ->
            File(repo, name).writeText("content of $name")
        }
        exec(
            cmd = "git add ${remoteFiles.joinToString(" ")} && git commit -m '$commitMessage' && git push origin master:master",
            cwd = repo,
        )
        return commitMessage
    }

    private fun exec(cmd: String, cwd: File) {
        val process = ProcessBuilder(
            "sh", "-c",
            cmd
        )
            .directory(cwd)
            .start()
        val retCode = process.waitFor()
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }

        println("Exit code: $retCode")
        println("Standard Output:\n$stdout")
        println("Standard Error:\n$stderr")
        Assert.assertEquals(0, retCode)
    }

    private fun locateServerSideDir(): File {
        val fixedCandidates = listOf(File("/server-side")) // mounted in client-side CI container
        for (candidate in fixedCandidates) {
            if (File(candidate, "localrun_for_tests.sh").exists()) {
                return candidate
            }
        }

        var dir: File? = File(System.getProperty("user.dir"))
        while (dir != null) {
            val script = File(dir, "server-side/localrun_for_tests.sh")
            if (script.exists()) {
                return script.parentFile
            }
            dir = dir.parentFile
        }
        error(
            "Could not locate server-side directory from user.dir=" +
                System.getProperty("user.dir")
        )
    }
}