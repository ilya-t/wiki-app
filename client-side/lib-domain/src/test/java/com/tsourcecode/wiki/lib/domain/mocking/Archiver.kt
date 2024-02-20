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
        if (src.isDirectory) {
            zipDirectory(src, dst, rootDir)
            return
        }
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dst))).use { zipOutputStream ->
            FileInputStream(src).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    val zipEntry = ZipEntry(rootDir + "/" + src.name)
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

    private fun zipDirectory(srcDir: File, dstFile: File, rootDir: String) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(dstFile))).use { zipOutputStream ->
            zip(srcDir, srcDir, zipOutputStream, rootDir)
        }
    }

    private fun zip(
        baseDir: File,
        currentFile: File,
        zipOutputStream: ZipOutputStream,
        rootDir: String
    ) {
        if (currentFile.isDirectory) {
            currentFile.listFiles()?.forEach { file ->
                zip(baseDir, file, zipOutputStream, rootDir)
            }
        } else {
            FileInputStream(currentFile).use { fileInputStream ->
                BufferedInputStream(fileInputStream).use { bufferedInputStream ->
                    val entryPath = baseDir.toPath().relativize(currentFile.toPath()).toString()
                        .replace("\\", "/")
                    val zipEntry = ZipEntry(rootDir + "/" + entryPath)
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

