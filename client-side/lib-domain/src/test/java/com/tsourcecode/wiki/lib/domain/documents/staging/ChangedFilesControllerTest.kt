package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.GlobalScope
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File
import java.net.URI

class ChangedFilesControllerTest {
    private val document = mock<Document> {
        on { relativePath } doReturn "docs/README.md"
    }

    private val worker = GlobalScope
    private val project = Project(
        id = "test",
        name = "test_project",
        filesDir = File("/tmp/${System.currentTimeMillis()}"),
        serverUri = URI.create(""),
        repoUri = "",
    )

    private val underTest = ChangedFilesController(
            project,
            worker,
    )

    @Test
    fun `synced file is not longer changed`() {
        underTest.markChanged(document, "content")
        underTest.notifyFileSynced(document)
        Assert.assertFalse(underTest.isChanged(document))
    }

    @Test
    fun `changed file marking works`() {
        underTest.markChanged(document, "content")
        Assert.assertTrue(underTest.isChanged(document))
    }

    @Test
    fun `file content changed`() {
        underTest.markChanged(document, "updated content")
        val fileContent = underTest.getChangedFile(document)!!.readText()
        Assert.assertTrue("Wrong file content: '$fileContent'",
            fileContent.contains("updated content"))
    }

    @Test
    fun `changed files scanned at start`() {
        underTest.markChanged(document, "updated content")

        val underTest2 = ChangedFilesController(
            project,
            worker,
        )

        val fileContent = underTest2.getChangedFile(document)!!.readText()
        Assert.assertTrue("Wrong file content: '$fileContent'",
            fileContent.contains("updated content"))
    }

}
