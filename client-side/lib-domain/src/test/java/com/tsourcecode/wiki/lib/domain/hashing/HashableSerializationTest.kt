package com.tsourcecode.wiki.lib.domain.hashing

import com.tsourcecode.wiki.lib.domain.backend.api.SyncApiPayload
import org.junit.Assert
import org.junit.Test
import java.io.File

class HashableSerializationTest {
    @Test
    fun `single file serialization`() {
        val origin = DirHash(
                File("root"), "__", listOf(
                FileHash(File("./README.md"), "?"),
                DirHash(File("./content"), "??", listOf(
                        FileHash(File("./c1"), "1"),
                        FileHash(File("./c2"), "2"),
                )),
        ))
        val json = SyncApiPayload.toBody(listOf(origin))
        val expected = """[{"name":"root","hash":"__","files":[{"name":"README.md","hash":"?"},{"name":"content","hash":"??","files":[{"name":"c1","hash":"1"},{"name":"c2","hash":"2"}]}]}]"""
        Assert.assertEquals(expected, json)
//TODO(deserialization not supported yet)
//        val deserialized = Json.decodeFromString(DirHash.serializer(), json)
//        Assert.assertEquals("intermediate json: $json", origin, deserialized)
    }

    @Test
    fun `collection serialization`() {
        val origin = listOf(
                FileHash(File("./README.md"), "?"),
                DirHash(File("./content"), "??", listOf(
                        FileHash(File("./c1"), "1"),
                        FileHash(File("./c2"), "2"),
                )),
        )

        val json = SyncApiPayload.toBody(origin)
        val expected = """[{"name":"README.md","hash":"?"},{"name":"content","hash":"??","files":[{"name":"c1","hash":"1"},{"name":"c2","hash":"2"}]}]"""
        Assert.assertEquals(expected, json)
//TODO(deserialization not supported yet)
//        val deserialized = Json.decodeFromString(DirHash.serializer(), json)
//        Assert.assertEquals("intermediate json: $json", origin, deserialized)
    }
}
