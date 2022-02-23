package com.tsourcecode.wiki.lib.domain.project

import java.io.File
import java.net.URL

class Project(
    val dir: File,
    val url: URL
) {
    val repo: File = File(dir.absolutePath + "/repo")
}
