package com.tsourcecode.wiki.lib.domain.hashing

import com.tsourcecode.wiki.lib.domain.hashing.HashUtils.getCheckSumFromFile
import com.tsourcecode.wiki.lib.domain.hashing.HashUtils.getCheckSumFromString
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

class ElementHashProvider(
        private val project: Project,
        private val workerScope: CoroutineScope,
) {
    private var validCache: List<Hashable> = emptyList()

    fun notifyProjectFullySynced() {
        workerScope.launch {
            validCache = calculateHashes(createDigest())
        }
    }

    private fun createDigest(): MessageDigest = MessageDigest.getInstance("SHA-1")

    suspend fun getHashes(): List<Hashable> {
        return withContext(workerScope.coroutineContext) {
            val result = validCache

            if (result.isNotEmpty()) {
                return@withContext result
            }
            val hashes = calculateHashes(createDigest())
            validCache = hashes
            hashes
        }
    }

    /**
     * Note! Not sharing single digest per instance to survive multithreaded environments.
     */
    private fun calculateHashes(digest: MessageDigest): List<Hashable> {
        val filesList = project.repo.listFiles()?.toList() ?: emptyList()
        return filesList.map {
            it.extractHashes(digest)
        }
    }

    private fun File.extractHashes(digest: MessageDigest): Hashable {
        return if (this.isDirectory) {
            val filesList = this.listFiles()?.toList() ?: emptyList()
            val fileHashes = filesList.map {
                it.extractHashes(digest)
            }

            val compositeHash = fileHashes
                    .asSequence()
                    .map { it.hash }
                    .sorted()
                    .reduce { acc, s -> acc + s }

            DirHash(this.name, getCheckSumFromString(digest, compositeHash), fileHashes)
        } else {
            FileHash(this.name, getCheckSumFromFile(digest, this))
        }
    }
}

/**
 * Thanks to: https://gist.github.com/LongClipeus/84db46e7d9714f67c4cbc40a67c8be1e
 */
private object HashUtils {
    const val STREAM_BUFFER_LENGTH = 1024

    fun getCheckSumFromString(digest: MessageDigest, s: String): String {
        digest.reset()
        digest.update(s.toByteArray())
        val byteArray = digest.digest()
        val hexCode = StringUtils.encodeHex(byteArray, true)
        return String(hexCode)
    }

    fun getCheckSumFromFile(digest: MessageDigest, file: File): String {
        digest.reset()
        val fis = FileInputStream(file)
        val byteArray = updateDigest(digest, fis).digest()
        fis.close()
        val hexCode = StringUtils.encodeHex(byteArray, true)
        return String(hexCode)
    }

    /**
     * Reads through an InputStream and updates the digest for the data
     *
     * @param digest The MessageDigest to use (e.g. MD5)
     * @param data Data to digest
     * @return the digest
     */
    private fun updateDigest(digest: MessageDigest, data: InputStream): MessageDigest {
        val buffer = ByteArray(STREAM_BUFFER_LENGTH)
        var read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
        while (read > -1) {
            digest.update(buffer, 0, read)
            read = data.read(buffer, 0, STREAM_BUFFER_LENGTH)
        }
        return digest
    }

}

private object StringUtils {

    /**
     * Used to build output as Hex
     */
    private val DIGITS_LOWER =
            charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    /**
     * Used to build output as Hex
     */
    private val DIGITS_UPPER =
            charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F')

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @param toLowerCase `true` converts to lowercase, `false` to uppercase
     * @return A char[] containing hexadecimal characters in the selected case
     */
    fun encodeHex(data: ByteArray, toLowerCase: Boolean): CharArray {
        return encodeHex(data, if (toLowerCase) DIGITS_LOWER else DIGITS_UPPER)
    }

    /**
     * Converts an array of bytes into an array of characters representing the hexadecimal values of each byte in order.
     * The returned array will be double the length of the passed array, as it takes two characters to represent any
     * given byte.
     *
     * @param data a byte[] to convert to Hex characters
     * @param toDigits the output alphabet (must contain at least 16 chars)
     * @return A char[] containing the appropriate characters from the alphabet
     *         For best results, this should be either upper- or lower-case hex.
     */
    fun encodeHex(data: ByteArray, toDigits: CharArray): CharArray {
        val l = data.size
        val out = CharArray(l shl 1)
        // two characters form the hex value.
        var i = 0
        var j = 0
        while (i < l) {
            out[j++] = toDigits[0xF0 and data[i].toInt() ushr 4]
            out[j++] = toDigits[0x0F and data[i].toInt()]
            i++
        }
        return out
    }
}
