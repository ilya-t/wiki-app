package com.tsourcecode.wiki.lib.domain.backend

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object Decompressor {
    fun decompress(zipFile: String, outputDir: String, logger: ((message: String) -> Unit)? = null) {
        logger?.invoke("preparing decompression dir: $outputDir")
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
                logger?.invoke(outputFile.toString())
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

                if (!outputFile.exists()) {
                    logger?.invoke("File not decompressed: $outputFile")
                }
            }
        } != null) { /*all job at also*/}
        zipInputStream.close()
        logger?.invoke("decompression completed. output dir: $outputDir")
    }

    private fun tryCreateDir(dir: String) {
        val f = File(dir)
        if (!f.isDirectory) {
            f.mkdirs()
        }
    }
}