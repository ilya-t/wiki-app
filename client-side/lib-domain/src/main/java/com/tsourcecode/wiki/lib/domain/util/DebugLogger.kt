package com.tsourcecode.wiki.lib.domain.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DebugLogger(
    private val logFile: File,
    private val scope: CoroutineScope,
) {
    companion object {
        private const val MAX_LINES = 10_000
        private const val DEBOUNCE_MS = 500L
        private val CHARSET: Charset = Charset.forName("UTF-8")
    }

    /**
     * Optional side-effect (e.g. Android Logcat output).
     */
    var impl: (String) -> Unit = {}

    private val lock = ReentrantLock()

    @Volatile
    private var linesCount: Int = 0
    private var pendingLines = mutableListOf<String>()
    private var flushJob: Job? = null
    private var pendingFlush = false
    private val initJob: Job

    init {
        initJob = scope.launch {
            if (logFile.exists()) {
                val allLines = logFile.readLines(CHARSET)
                val count = allLines.size
                if (count > MAX_LINES) {
                    val trimmed = allLines.drop(count - MAX_LINES)
                    logFile.writeText(trimmed.joinToString("\n") + "\n", CHARSET)
                    linesCount = MAX_LINES
                } else {
                    linesCount = count
                }
            } else {
                logFile.parentFile?.mkdirs()
                linesCount = 0
            }
        }
    }

    fun log(vararg args: Any?) {
        val message = args.joinToString(" ")
        impl.invoke(message)
        lock.withLock {
            pendingLines.add(message)
            if (!pendingFlush) {
                pendingFlush = true
                flushJob = scope.launch {
                    delay(DEBOUNCE_MS)
                    flushToFile()
                    pendingFlush = false
                }
            }
        }
    }

    /**
     * Force an immediate flush of buffered logs to disk.
     */
    fun flush() {
        lock.withLock {
            flushJob?.cancel()
            flushToFile()
            pendingFlush = false
        }
    }

    /**
     * Read all persisted logs from the file.
     */
    suspend fun readLogs(): List<String> {
        initJob.join()
        flush()
        return readFileLines()
    }

    private fun flushToFile() {
        if (pendingLines.isEmpty()) return
        val lines = pendingLines.toList()
        pendingLines.clear()
        appendLines(lines)
        linesCount += lines.size
        if (linesCount > MAX_LINES) {
            trimToMaxLines()
        }
    }

    private fun appendLines(lines: List<String>) {
        logFile.parentFile?.mkdirs()
        RandomAccessFile(logFile, "rw").use { raf ->
            raf.seek(raf.length())
            for (line in lines) {
                raf.write((line + "\n").toByteArray(CHARSET))
            }
        }
    }

    private fun trimToMaxLines() {
        val allLines = readFileLines()
        if (allLines.size <= MAX_LINES) return
        val trimmed = allLines.drop(allLines.size - MAX_LINES)
        logFile.writeText(trimmed.joinToString("\n") + "\n", CHARSET)
        linesCount = MAX_LINES
    }

    private fun readFileLines(): List<String> {
        if (!logFile.exists()) return emptyList()
        return logFile.readLines(CHARSET)
    }
}
