package com.tsourcecode.wiki.lib.domain.documents

import org.junit.Assert
import org.junit.Test
import java.io.File

class DocumentTest {
    @Test
    fun `relative path`() {
        val d = Document(
                projectDir = File("/tmp/repo/dir"),
                origin = File("/tmp/repo/dir/src/README.md")
        )

        Assert.assertEquals("src/README.md", d.relativePath)
    }

    @Test
    fun `relative path (trailing slash)`() {
        val d = Document(
                projectDir = File("/tmp/repo/dir/"),
                origin = File("/tmp/repo/dir/README.md")
        )

        Assert.assertEquals("README.md", d.relativePath)
    }
}