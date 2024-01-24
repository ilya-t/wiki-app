package com.tsourcecode.wiki.lib.domain.backend

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object Decompressor {
    fun decompress(zipFile: String, outputDir: String, logger: ((String) -> Unit)? = null) {
        with(File(outputDir)) {
            if (!exists()) {
                mkdirs()
            }
        }
        val fileInputStream = FileInputStream(zipFile)
        val zipInputStream = ZipInputStream(fileInputStream)
        var zipEntry: ZipEntry

        while (zipInputStream.nextEntry.also {
            zipEntry = it?:return@also
            if (zipEntry.isDirectory) {
                tryCreateDir(outputDir + "/"+ zipEntry.name)
            } else {
                val outputFile = File(outputDir + "/" + zipEntry.name)
                logger?.invoke("(${zipEntry.name})")
                if (!outputFile.parentFile.exists()) {
                    outputFile.parentFile.mkdirs()
                }
                val outputStream = FileOutputStream(outputFile)
                var data = zipInputStream.read()
                while (data != -1) {
                    outputStream.write(data)
                    data = zipInputStream.read()
                }
                zipInputStream.closeEntry()
                outputStream.close()
            }
        } != null) { /*all job at also*/}
        zipInputStream.close()
    }

    private fun tryCreateDir(dir: String) {
        val f = File(dir)
        if (!f.isDirectory) {
            f.mkdirs()
        }
    }
}