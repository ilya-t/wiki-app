package com.tsourcecode.wiki.lib.domain.util

import com.tsourcecode.wiki.lib.domain.documents.Document
import com.tsourcecode.wiki.lib.domain.project.Project
import java.net.URI
import java.net.URLEncoder

object NavigationUtils {
    fun openDocument(p: Project, d: Document): URI {
        val encodedName = URLEncoder.encode(p.name, "UTF-8")
        val encodedPath = d.relativePath.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8") }
        return URI("edit://${encodedName}/${encodedPath}")
    }

}