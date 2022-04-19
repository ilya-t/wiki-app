package com.tsourcecode.wiki.app.documents

import com.tsourcecode.wiki.lib.domain.tests.OpenInTest
import java.io.File


sealed class Element(
        val file: File,
)

@OpenInTest
data class Document(
    private val projectDir: File,
    /**
     * Original unchanged and synced locally file.
     * Do not open access to absolute path!
     */
    private val origin: File,
) : Element(origin) {
    val relativePath: String = run {
        val start = projectDir.absolutePath.length + 1

        if (start < origin.absolutePath.length) {
            origin.absolutePath.substring(start)
        } else {
            origin.absolutePath
        }
    }
}

data class Folder(
        private val f: File,
        val elements: List<Element>,
) : Element(f) {
    fun find(filePath: String): Element? {
        if (filePath.isEmpty()) {
            return this
        }

        var result: Element? = this

        filePath.split("/").forEach { part ->
            val capturedResult = result
            if (capturedResult is Folder) {
                result = capturedResult.elements.firstOrNull { it.file.name == part }
            } else {
                return null
            }
        }

        return result
    }

    val documents: List<Document> = elements
            .filterIsInstance<Document>()
}
