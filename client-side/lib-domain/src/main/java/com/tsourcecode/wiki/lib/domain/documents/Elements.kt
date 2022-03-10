package com.tsourcecode.wiki.app.documents

import com.tsourcecode.wiki.lib.domain.tests.OpenInTest
import java.io.File


sealed class Element(
        val file: File,
)

@OpenInTest
data class Document(
        private val projectDir: File,
        private val f: File,
) : Element(f) {
    val relativePath: String = run {
        val start = projectDir.absolutePath.length + 1

        if (start < f.absolutePath.length) {
            f.absolutePath.substring(start)
        } else {
            f.absolutePath
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
