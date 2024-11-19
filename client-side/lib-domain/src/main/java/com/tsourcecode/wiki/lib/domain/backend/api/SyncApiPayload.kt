package com.tsourcecode.wiki.lib.domain.backend.api

import com.tsourcecode.wiki.lib.domain.hashing.DirHash
import com.tsourcecode.wiki.lib.domain.hashing.FileHash
import com.tsourcecode.wiki.lib.domain.hashing.Hashable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray

object SyncApiPayload {
    fun toBody(files: List<Hashable>): String {
        return SyncPayloadSerializer.serializeList(files.map {
            it.toHashableElement()
        })
    }
}

private fun Hashable.toHashableElement(): SyncPayloadSerializer.HashableElement {
    return when (this) {
        is DirHash -> SyncPayloadSerializer.DirElement(this.file.name, this.hash,
            this.fileHashes.map { it.toHashableElement() })
        is FileHash -> SyncPayloadSerializer.FileElement(this.file.name, this.hash)
    }
}

object SyncPayloadSerializer : KSerializer<SyncPayloadSerializer.HashableElement> {
    @Serializable(with = SyncPayloadSerializer::class)
    sealed interface HashableElement {
        val name: String
        val hash: String
    }

    @Serializable
    data class FileElement(
        @SerialName("name")
        override val name: String,
        @SerialName("hash")
        override val hash: String,
    ) : HashableElement

    @Serializable
    data class DirElement(
        @SerialName("name")
        override val name: String,
        @SerialName("hash")
        override val hash: String,
        @SerialName("files")
        val fileHashes: List<HashableElement>) : HashableElement

    override fun deserialize(decoder: Decoder): HashableElement {
        // https://github.com/Kotlin/kotlinx.serialization/issues/1417#issuecomment-1021545236
        TODO("Next try at 1.6.20")
        return try {
            decoder.decodeSerializableValue(DirElement.serializer())
        } catch (e: SerializationException) {
            decoder.decodeSerializableValue(FileElement.serializer())
        }
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: HashableElement) {
        return when (value) {
            is DirElement -> encoder.encodeSerializableValue(DirElement.serializer(), value)
            is FileElement -> encoder.encodeSerializableValue(FileElement.serializer(), value)
        }
    }

    fun serializeList(list: List<HashableElement>): String {
        val array = buildJsonArray {
            list.forEach {
                val element = when (it) {
                    is DirElement -> Json.encodeToJsonElement(DirElement.serializer(), it)
                    is FileElement -> Json.encodeToJsonElement(FileElement.serializer(), it)
                }
                add(element)
            }
        }

        return array.toString()
    }
}
