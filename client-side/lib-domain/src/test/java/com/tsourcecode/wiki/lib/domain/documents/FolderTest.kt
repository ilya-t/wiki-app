package com.tsourcecode.wiki.lib.domain.documents

import org.junit.Assert
import org.junit.Test
import java.io.File

class FolderTest {
    private val rootFile = File("/tmp")
    private val underTest = Folder(
            rootFile,
            listOf<Element>(
                    Folder(
                            File("/tmp/folder"),
                            listOf(
                                    Document(
                                            File("/tmp/folder"),
                                            File("README.md"))
                            )
                    )
            )
    )

    @Test
    fun `find file by relative path`() {
        val d = underTest.find("folder/README.md")
        Assert.assertEquals("README.md", d!!.file.name)
    }

    @Test
    fun `nothing found with single slash`() {
        val f = underTest.find("/")
        Assert.assertNull(f)
    }

    @Test
    fun `root returned with empty result`() {
        val f = underTest.find("")
        Assert.assertEquals(rootFile, f!!.file)

    }

    @Test
    fun `find folder by relative path`() {
        val f = underTest.find("folder")
        Assert.assertEquals("folder", f!!.file.name)
    }

    @Test
    fun `not finding folder when path not relative`() {
        val f =
        Assert.assertNull(underTest.find("/folder"))
    }
}