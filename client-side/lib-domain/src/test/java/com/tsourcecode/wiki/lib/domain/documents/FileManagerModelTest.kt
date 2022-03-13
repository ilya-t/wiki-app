package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.app.documents.Document
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.QuickStatusController
import com.tsourcecode.wiki.lib.domain.project.Project
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.File
import java.net.URI

class FileManagerModelTest {
    private val uri = argumentCaptor<URI>()
    private val appNavigator = mock<AppNavigator> {
        on { open(uri.capture()) } doAnswer { /*nothing*/ }
    }
    private val quickStatusController = mock<QuickStatusController>()
    private val project = mock<Project> {
        on { name } doReturn "project_name"
    }

    private val underTest = FileManagerModel(
        appNavigator,
        quickStatusController,
    )

    @Test
    fun `opening document with spaces in name`() {
        val projectDir = File("/tmp/project/root")
        underTest.open(project, Document(
                projectDir, File(projectDir, "folder with spaces/1 file with spaces.txt")
        ))

        Assert.assertEquals("edit://project_name/folder+with+spaces/1+file+with+spaces.txt", uri.lastValue.toString())
    }
}