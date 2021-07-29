package com.tsourcecode.wiki.app.documents

import java.io.File


sealed class Element(
        val file: File,
)

data class Document(
        private val projectDir: File,
        private val f: File,
) : Element(f) {
    val relativePath: String = f.absolutePath.substring(projectDir.absolutePath.length + 1)
}

data class Folder(
        private val f: File,
        val elements: List<Element>,
) : Element(f) {
    val documents: List<Document> = elements
            .filterIsInstance<Document>()
}
