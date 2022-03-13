package com.tsourcecode.wiki.lib.domain.project

import com.tsourcecode.wiki.lib.domain.tests.OpenInTest
import java.io.File
import java.net.URI

@OpenInTest
class Project(
    val id: String,
    val name: String,
    filesDir: File,
    val serverUri: URI,
    val repoUri: String
) {
    val dir = File(filesDir, id)
    val repo: File = File(dir, "repo")
}
