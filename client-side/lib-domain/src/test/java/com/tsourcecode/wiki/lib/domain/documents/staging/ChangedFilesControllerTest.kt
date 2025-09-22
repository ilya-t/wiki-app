package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.project.Project
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import kotlinx.coroutines.GlobalScope
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File
import java.net.URI

class ChangedFilesControllerTest {
    private val projectStorage = mock<KeyValueStorage>()
    private val elementHashProvider = mock<ElementHashProvider>()

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
        projectStorage,
        elementHashProvider,
        mock(),
    )
//
//    @Test
//    fun `synced file is not longer changed`() {
//        underTest.markChanged(document, "content")
//        underTest.notifyFileSynced(document)
//        Assert.assertFalse(underTest.isChanged(document))
//    }
//
//    @Test
//    fun `changed file marking works`() {
//        underTest.markChanged(document, "content")
//        Assert.assertTrue(underTest.isChanged(document))
//    }
//
//    @Test
//    fun `file content changed`() {
//        underTest.markChanged(document, "updated content")
//        val fileContent = underTest.getChangedFile(document)!!.readText()
//        Assert.assertTrue("Wrong file content: '$fileContent'",
//            fileContent.contains("updated content"))
//    }
//
//    @Test
//    fun `changed files scanned at start`() {
//        underTest.markChanged(document, "updated content")
//
//        val underTest2 = ChangedFilesController(
//            project,
//            worker,
//            logger,
//        )
//
//        val fileContent = underTest2.getChangedFile(document)!!.readText()
//        Assert.assertTrue("Wrong file content: '$fileContent'",
//            fileContent.contains("updated content"))
//    }
//
}
