package com.tsourcecode.wiki.lib.domain.hashing

import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test

class HashableSerializationTest {
    @Test
    fun `single file serialization`() {
        val origin = DirHash(
                "root", "__", listOf(
                FileHash("README.md", "?"),
                DirHash("content", "??", listOf(
                        FileHash("c1", "1"),
                        FileHash("c2", "2"),
                )),
        ))

        val json = Json.encodeToString(DirHash.serializer(), origin)
        val expected = """{"name":"root","hash":"__","files":[{"name":"README.md","hash":"?"},{"name":"content","hash":"??","files":[{"name":"c1","hash":"1"},{"name":"c2","hash":"2"}]}]}"""
        Assert.assertEquals(expected, json)
//TODO(deserialization not supported yet)
//        val deserialized = Json.decodeFromString(DirHash.serializer(), json)
//        Assert.assertEquals("intermediate json: $json", origin, deserialized)
    }

    @Test
    fun `collection serialization`() {
        val origin = listOf(
                FileHash("README.md", "?"),
                DirHash("content", "??", listOf(
                        FileHash("c1", "1"),
                        FileHash("c2", "2"),
                )),
        )

        val json = FileHashSerializable.serializeList(origin)
        val expected = """[{"name":"README.md","hash":"?"},{"name":"content","hash":"??","files":[{"name":"c1","hash":"1"},{"name":"c2","hash":"2"}]}]"""
        Assert.assertEquals(expected, json)
//TODO(deserialization not supported yet)
//        val deserialized = Json.decodeFromString(DirHash.serializer(), json)
//        Assert.assertEquals("intermediate json: $json", origin, deserialized)
    }
}
