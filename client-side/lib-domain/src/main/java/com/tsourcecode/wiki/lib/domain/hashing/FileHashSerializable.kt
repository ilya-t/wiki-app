package com.tsourcecode.wiki.lib.domain.hashing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray

object FileHashSerializable : KSerializer<Hashable> {
    override fun deserialize(decoder: Decoder): Hashable {
        // https://github.com/Kotlin/kotlinx.serialization/issues/1417#issuecomment-1021545236
        TODO("Next try at 1.6.20")
        return try {
            decoder.decodeSerializableValue(DirHash.serializer())
        } catch (e: SerializationException) {
            decoder.decodeSerializableValue(FileHash.serializer())
        }
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Hashable) {
        return when (value) {
            is DirHash -> encoder.encodeSerializableValue(DirHash.serializer(), value)
            is FileHash -> encoder.encodeSerializableValue(FileHash.serializer(), value)
        }
    }

    fun serializeList(list: List<Hashable>): String {
        val array = buildJsonArray {
            list.forEach {
                val element = when (it) {
                    is DirHash -> Json.encodeToJsonElement(DirHash.serializer(), it)
                    is FileHash -> Json.encodeToJsonElement(FileHash.serializer(), it)
                }
                add(element)
            }
        }

        return array.toString()
    }
}
