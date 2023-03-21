package com.tsourcecode.wiki.lib.domain.mocking
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Archiver {
    fun zipFile(src: File, dst: File, rootDir: String) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dst))).use { zipOutputStream ->
            FileInputStream(src).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    val zipEntry = ZipEntry(rootDir+ "/"+src.name)
                    zipOutputStream.putNextEntry(zipEntry)

                    val buffer = ByteArray(1024)
                    var length: Int
                    while (bufferedInputStream.read(buffer).also { length = it } > 0) {
                        zipOutputStream.write(buffer, 0, length)
                    }
                }
            }
        }
    }
}

