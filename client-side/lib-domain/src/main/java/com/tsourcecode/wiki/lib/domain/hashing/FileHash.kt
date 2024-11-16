package com.tsourcecode.wiki.lib.domain.hashing

import java.io.File

sealed interface Hashable {
    val file: File
    val hash: String
}

data class FileHash(
        override val file: File,
        override val hash: String,
) : Hashable

data class DirHash(
        override val file: File,
        override val hash: String,
        val fileHashes: List<Hashable>) : Hashable
