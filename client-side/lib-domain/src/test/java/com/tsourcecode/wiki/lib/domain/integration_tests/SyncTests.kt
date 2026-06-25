package com.tsourcecode.wiki.lib.domain.integration_tests

import com.tsourcecode.wiki.lib.domain.TestDomainComponentFactory
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private val testDir = File("/tmp/syncTests_${UUID.randomUUID()}").also {
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

    private suspend fun openFirstProjectStatus(): StatusModel {
        val projects: List<ConfigScreenItem> = importProjects()
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

    private suspend fun importProjects(): List<ConfigScreenItem> {
        domain.viewModels.configScreenModel.submitImport(
            ConfigScreenItem.ImportFrom(
                projectUrl = serverController.serverUrl
            )
        ).wait()
        return domain.viewModels.configScreenModel.data.first {
            it.filterIsInstance<ConfigScreenItem.PreviewElement>().isNotEmpty()
        }
    }

    private suspend fun captureTestProject(): Project {
        val projects = domain.projectsRepository.data.first { it.isNotEmpty() }
        return projects.first { it.name == TEST_PROJECT }
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

private class ServerController(
    private val serverSideDir: File,
    private val serverFiles: File,
    private val alias: String,
) {
    val serverUrl = "http://127.0.0.1:8181"
    private val serverProcess: Process

    companion object {
        private const val HEARTBEAT_TIMEOUT_MS = 120_000L

        fun ensureStopped(serverSideDir: File) {
            runScript(serverSideDir, "./localstop.sh")
        }

        private fun runScript(serverSideDir: File, script: String) {
            val process = ProcessBuilder("sh", "-c", script)
                .directory(serverSideDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val retCode = process.waitFor()
            if (output.isNotBlank()) {
                println(output.trim())
            }
            if (retCode != 0) {
                println("$script exited with code $retCode")
            }
        }
    }

    init {
        serverFiles.mkdirs()

        val cmd = "./localrun_for_tests.sh $serverFiles '$alias'"
        serverProcess = ProcessBuilder(
            "sh", "-c",
            cmd
        )
            .directory(serverSideDir)
            .start()
    }
    fun start() {

    }

    fun waitHeartbeats() = runBlocking {
        val timeout = System.currentTimeMillis() + HEARTBEAT_TIMEOUT_MS
        while (System.currentTimeMillis() < timeout) {
            if (checkHeartbeat()) return@runBlocking
            delay(50)
        }
        throw IllegalStateException(
            "Server did not respond with 200 within ${HEARTBEAT_TIMEOUT_MS / 1000} seconds"
        )
    }

    private fun checkHeartbeat(): Boolean {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$serverUrl/api/health")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val body = response.body?.charStream()?.readText() ?: return false
                val status = Json.decodeFromString(Heartbeat.serializer(), body)
                println("heartbeat response body: '$body'")
                return status.alias == alias
            }
        } catch (e: Exception) {
            println("heartbeat failed: '$e'")

            // Ignore exceptions and retry
        }
        return false
    }

    private fun waitHeartbeatsStop() = runBlocking {
        val timeout = System.currentTimeMillis() + 5000 // 5 seconds timeout
        while (System.currentTimeMillis() < timeout) {
            if (!checkHeartbeat()) return@runBlocking
            delay(50)
        }
    }

    fun stop() {
        ensureStopped(serverSideDir)
        if (serverProcess.isAlive) {
            serverProcess.destroy()
        } else {
            println("Server already finished with exit code: ${serverProcess.exitValue()}")
            serverProcess.errorStream.bufferedReader().use { reader ->
                println("Server stderr: ${reader.readText()}")
            }
        }
        waitHeartbeatsStop()
    }
}

@Serializable
private data class Heartbeat(
    val alias: String,
    val status: String,
)