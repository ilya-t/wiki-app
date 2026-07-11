package com.tsourcecode.wiki.lib.domain.integration_tests

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class ServerController(
    private val serverSideDir: File,
    private val serverFiles: File,
    private val alias: String,
) {
    val serverUrl = System.getenv("SYNC_TEST_SERVER_URL") ?: "http://127.0.0.1:8181"
    private val serverProcess: Process

    companion object {
        private const val HEARTBEAT_TIMEOUT_MS = 120_000L

        fun ensureStopped(serverSideDir: File) {
            runScript(serverSideDir, "./localstop.sh")
        }

        private fun runScript(serverSideDir: File, script: String) {
            val process = ProcessBuilder("sh", "-c", script)
                .directory(serverSideDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val retCode = process.waitFor()
            if (output.isNotBlank()) {
                println(output.trim())
            }
            if (retCode != 0) {
                println("$script exited with code $retCode")
            }
        }
    }

    init {
        serverFiles.mkdirs()

        val cmd = "./localrun_for_tests.sh $serverFiles '$alias'"
        serverProcess = ProcessBuilder(
            "sh", "-c",
            cmd
        )
            .directory(serverSideDir)
            .start()
    }
    fun start() {

    }

    fun waitHeartbeats() = runBlocking {
        val timeout = System.currentTimeMillis() + HEARTBEAT_TIMEOUT_MS
        while (System.currentTimeMillis() < timeout) {
            if (checkHeartbeat()) return@runBlocking
            delay(50)
        }
        throw IllegalStateException(
            "Server did not respond with 200 within ${HEARTBEAT_TIMEOUT_MS / 1000} seconds. "
        )
    }

    private fun checkHeartbeat(): Boolean {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("$serverUrl/api/health")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.code == 200) {
                val body = response.body?.charStream()?.readText() ?: return false
                val status = Json.Default.decodeFromString(Heartbeat.serializer(), body)
                println("heartbeat response body: '$body'")
                return status.alias == alias
            }
        } catch (e: Exception) {
            println("heartbeat failed: '$e'")

            // Ignore exceptions and retry
        }
        return false
    }

    private fun waitHeartbeatsStop() = runBlocking {
        val timeout = System.currentTimeMillis() + 5000 // 5 seconds timeout
        while (System.currentTimeMillis() < timeout) {
            if (!checkHeartbeat()) return@runBlocking
            delay(50)
        }
    }

    fun stop() {
        ensureStopped(serverSideDir)
        if (serverProcess.isAlive) {
            serverProcess.destroy()
        } else {
            println("Server already finished with exit code: ${serverProcess.exitValue()}")
            serverProcess.errorStream.bufferedReader().use { reader ->
                println("Server stderr: ${reader.readText()}")
            }
        }
        waitHeartbeatsStop()
    }
}

@Serializable
private data class Heartbeat(
    val alias: String,
    val status: String,
)
