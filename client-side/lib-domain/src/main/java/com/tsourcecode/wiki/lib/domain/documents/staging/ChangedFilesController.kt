package com.tsourcecode.wiki.lib.domain.documents.staging

import com.tsourcecode.wiki.lib.domain.hashing.DirHash
import com.tsourcecode.wiki.lib.domain.hashing.ElementHashProvider
import com.tsourcecode.wiki.lib.domain.hashing.FileHash
import com.tsourcecode.wiki.lib.domain.hashing.Hashable
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.StoredPrimitive
import com.tsourcecode.wiki.lib.domain.util.Logger
import kotlinx.serialization.json.Json

class ChangedFilesController(
    private val projectStorage: KeyValueStorage,
    private val elementHashProvider: ElementHashProvider,
    logger: Logger,
) {
    private val changesStorage = StoredPrimitive.string("changed_files", projectStorage)
    private val logger = logger.fork("changed-files-controller:")

    private fun toPersistentHashes(h: Hashable): List<PersistentFileHash> {
        return when (h) {
            is DirHash -> {
                val results = mutableListOf<PersistentFileHash>()
                results.add(h.asFileHash())
                h.fileHashes.forEach {
                    results.addAll(toPersistentHashes(it))
                }
                results
            }
            is FileHash -> listOf(h.asFileHash())
        }
    }

    suspend fun assumeCurrentFilesAreSynchronized() {
        val hashes: List<PersistentFileHash> = elementHashProvider.getHashes().flatMap { h: Hashable ->
            toPersistentHashes(h)
        }

        val encoded = Json.encodeToString(ChangedFilesSnapshot.serializer(),
            ChangedFilesSnapshot(hashes))
        logger.log { "synched files snapshot updated: $encoded" }
        changesStorage.value = encoded
    }

    suspend fun haveChanges(): Boolean {
        val snapshotString: String = changesStorage.value ?: run {
            logger.log { "assume local changes since last sync cause no changes snapshot exists!" }
            return true
        }
        val snapshot: ChangedFilesSnapshot = Json.decodeFromString<ChangedFilesSnapshot>(snapshotString)
        val savedHashes = snapshot.files.associate { it.path to it.hash }

        val hashes = elementHashProvider.getHashes()
        return hashes.any {
            val savedHash = savedHashes[it.file.absolutePath] ?: run {
                logger.log { "local changes detected since last sync at: '${it.file}' (new file)" }
                return true
            }
            val currentHash = it.hash
            val hasChanges = savedHash != currentHash
            if (hasChanges) {
                logger.log { "local changes detected since last sync at: '${it.file}'" }
            } else {
                logger.log { "no local changes detected for: '${it.file}', file content: '${it.file.readText()}'" }
            }
            hasChanges
        }.also {
            if (!it) {
                logger.log { "no local changes detected since last sync" }
            }

        }
    }
}

private fun Hashable.asFileHash() = PersistentFileHash(
    path = this.file.absolutePath,
    hash = this.hash,
)
