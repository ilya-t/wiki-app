package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import java.util.concurrent.ConcurrentHashMap

class InMemoryKeyValueStorage : KeyValueStorage {
    private val store = ConcurrentHashMap<String, String?>()
    override fun get(key: String): String? = store[key]

    override fun set(key: String, value: String?) {
        store[key] = value
    }

    override fun contains(key: String): Boolean = store.contains(key)
}