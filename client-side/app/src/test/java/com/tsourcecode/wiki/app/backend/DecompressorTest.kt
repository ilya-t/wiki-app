package com.tsourcecode.wiki.app.backend

import org.junit.Assert
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.RuntimeException
import java.lang.StringBuilder

class DecompressorTest {
    @Test
    fun smoke() {
        printOutput("pwd")
        val filesStructure = listOf(
                "root.file",
                "dir1/dir1_file0.file",
                "dir1/dir1_file1.file",
                "dir2/dir2_file0.file",
        )
        val zipFile = generateZipArchive(filesStructure)
        val outputDir = "/tmp/decompressed_" + System.currentTimeMillis()
        Decompressor.decompress(zipFile, outputDir)
        val outputContent = checkOutput("ls $outputDir")
        filesStructure.forEach {
            val extractedFile = "$outputDir/$it"
            Assert.assertTrue(
                    "Extracted file not exists: $extractedFile\ncontents: $outputContent",
                    File(extractedFile).exists()
            )
        }
    }

    private fun generateZipArchive(files: List<String>): String {
        val salt = System.currentTimeMillis()
        val filesDir = "/tmp/decomressor_test_$salt"
        val zipFile = "/tmp/decomressor_test_$salt.zip"
        println("Generating zip archive at $filesDir")
        printOutput("mkdir -p $filesDir")
        files.forEach {
            val fileName = "$filesDir/$it"
            printOutput("mkdir -p ${File(fileName).parent}")
            printOutput("echo \"${it} file content\" > $fileName")
        }
        printOutput("cd $filesDir && zip -r $zipFile ./*")
        return zipFile
    }

    private fun checkOutput(cmd: String): String {
        val processBuilder = ProcessBuilder()
        processBuilder.command("/bin/sh", "-c", "-x", cmd)
        val lines = StringBuilder()

        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val exitCode = process.waitFor()
        while (reader.readLine().also {
                    lines.append("\n$it")
                } != null) { /* nothing to do */
        }

        if (exitCode != 0) {
            throw RuntimeException(
                    """
                        Command: '$cmd' exited with: $exitCode
                        Output: ${lines}  
                        """.trimIndent()
            )
        }

        return lines.toString()
    }

    private fun printOutput(cmd: String) {
        println(cmd)
        println("out: " + checkOutput(cmd))
    }
}