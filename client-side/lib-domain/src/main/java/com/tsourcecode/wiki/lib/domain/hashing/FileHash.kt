package com.tsourcecode.wiki.lib.domain.hashing

sealed interface Hashable {
    val name: String
    val hash: String
}

class FileHash(
        override val name: String,
        override val hash: String,
) : Hashable

class DirHash(
        override val name: String,
        override val hash: String,
        val fileHashes: List<Hashable>) : Hashable
