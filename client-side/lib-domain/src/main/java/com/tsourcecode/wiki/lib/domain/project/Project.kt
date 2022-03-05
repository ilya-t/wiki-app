package com.tsourcecode.wiki.lib.domain.project

import java.io.File
import java.net.URL

class Project(
    val name: String,
    filesDir: File,
    val url: URL
) {
    val dir = File(filesDir, name)
    val repo: File = File(dir.absolutePath + "/repo")
}
