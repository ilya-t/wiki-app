package com.tsourcecode.wiki.lib.domain.storage

import com.tsourcecode.wiki.lib.domain.InMemoryKeyValueStorage
import org.junit.Assert
import org.junit.Test

private const val KEY = "key"

class StoredPrimitiveTest {
    private val storageProvider = InMemoryKeyValueStorage()

    @Test
    fun `save-n-restore - stringList`() {
        val sut1 = StoredPrimitive.stringList(KEY, storageProvider)
        val expected = listOf("smoke", "test")
        sut1.value = expected

        val sut2 = StoredPrimitive.stringList(KEY, storageProvider)
        Assert.assertArrayEquals(
            expected.toTypedArray(),
            sut2.value!!.toTypedArray()
        )
    }
}