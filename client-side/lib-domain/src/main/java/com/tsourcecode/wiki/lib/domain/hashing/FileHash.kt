package com.tsourcecode.wiki.lib.domain.hashing

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable(with = FileHashSerializable::class)
sealed interface Hashable {
    val name: String
    val hash: String
}

@Serializable
data class FileHash(
        @SerialName("name")
        override val name: String,
        @SerialName("hash")
        override val hash: String,
) : Hashable

@Serializable
data class DirHash(
        @SerialName("name")
        override val name: String,
        @SerialName("hash")
        override val hash: String,
        @SerialName("files")
        val fileHashes: List<Hashable>) : Hashable
