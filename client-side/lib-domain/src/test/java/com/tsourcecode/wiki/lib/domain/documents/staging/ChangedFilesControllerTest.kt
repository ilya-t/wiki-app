package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorage
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File

class ChangedFilesControllerTest {
    private val changedStorage = mock<PersistentStorage>()
    private val stagedStorage = mock<PersistentStorage>()
    private val storageProvider = mock<PersistentStorageProvider> {
        on { get("changed") } doReturn changedStorage
        on { get("staged") } doReturn stagedStorage
    }

    private val document = mock<Document> {
        on { relativePath } doReturn "README.md"
    }

    private val underTest = ChangedFilesController(
            changedFilesDir = File("/tmp/${System.currentTimeMillis()}"),
            persistentStorageProvider = storageProvider
    )

    @Test
    fun `staged file is changed`() {
        underTest.markChanged(document, "content")
        underTest.markStaged(document)
        Assert.assertTrue(underTest.isStaged(document))
        Assert.assertTrue(underTest.isChanged(document))
    }

    @Test
    fun `synced file is not longer changed`() {
        underTest.markChanged(document, "content")
        underTest.markStaged(document)
        underTest.notifyFileSynced(document)
        Assert.assertFalse(underTest.isStaged(document))
        Assert.assertFalse(underTest.isChanged(document))
    }

}
