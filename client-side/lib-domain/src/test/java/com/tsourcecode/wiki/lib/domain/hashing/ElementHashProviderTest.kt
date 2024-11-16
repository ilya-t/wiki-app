package com.tsourcecode.wiki.lib.domain.hashing

import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.io.File
import java.net.URI

class ElementHashProviderTest {
    @get:Rule val rule = TestName()

    private val project = Project(
            id = "id",
            name = "name",
            filesDir = File("/tmp/repo/${rule.methodName}"),
            serverUri = URI("http://url"),
            repoUri = "repo_uri"
    )
    private val fixedSha1File = File(project.repo, "file_with_fixed_sha1")
    private val contentDir = File(project.repo, "content")

    private val scope = GlobalScope// TestScope()
    private val underTest = ElementHashProvider(
            project,
            scope,
    )

    @Test
    fun `basic structure generation`() {
        generateProjectStructure()
        underTest.notifyProjectFullySynced()
        val results = runBlocking {
            underTest.getHashes()
        }

        Assert.assertEquals(project.repo.listFiles()!!.size, results.size)
    }

    @Test
    fun `hashing generation`() {
        generateProjectStructure()
        underTest.notifyProjectFullySynced()
        val results = runBlocking {
            underTest.getHashes()
        }

        val sha1file = results.find { it.name == fixedSha1File.name }!!

        val message = "File content: '${fixedSha1File.readText()}'"
        Assert.assertEquals(message,
                "0758fe8844f102aaa616c30c94ea4f8eb9326b06", sha1file.hash)
    }

    @Test
    fun `nested structure generation`() {
        generateProjectStructure()
        underTest.notifyProjectFullySynced()
        val results = runBlocking {
            underTest.getHashes()
        }

        val content = results.find { it.name == contentDir.name } as DirHash

        Assert.assertEquals(2, content.fileHashes.size)
        Assert.assertEquals("content_file_1", content.fileHashes.find { it.name == "content_file_1" }!!.name)
        Assert.assertEquals("content_file_2", content.fileHashes.find { it.name == "content_file_2" }!!.name)
    }

    private fun generateProjectStructure() {
        Assert.assertTrue(project.repo.deleteRecursively())
        Assert.assertTrue(project.repo.mkdirs())
        contentDir.mkdirs()
        File(contentDir, "content_file_1").writeText("some_content_1")
        File(contentDir, "content_file_2").writeText("some_content_2")

        fixedSha1File.writeText("do not modify this file! is's sha1 is used under tests \n")
        File(project.repo, "README.md").writeText("# Readme sample")
    }
}
