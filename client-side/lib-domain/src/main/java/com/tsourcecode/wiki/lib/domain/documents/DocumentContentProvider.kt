package com.tsourcecode.wiki.lib.domain.documents

import com.tsourcecode.wiki.app.documents.Document
import java.io.*

class DocumentContentProvider {
    private val inMemoryStore = mutableMapOf<Document, String>()

    fun getContent(d: Document): String {
        return inMemoryStore.getOrPut(d) {
            getStringFromFile(d.file)
        }
    }

    private fun convertStreamToString(inputStream: InputStream?): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val sb = StringBuilder()
        while (reader.readLine().also {
                    if (it != null) {
                        sb.append(it)
                    }
                    sb.append("\n")
        } != null) { /*nothing here*/ }
        reader.close()
        return sb.toString()
    }

    private fun getStringFromFile(fl: File): String {
        val fin = FileInputStream(fl)
        val ret = convertStreamToString(fin)
        //Make sure you close all streams.
        fin.close()
        return ret
    }
}
