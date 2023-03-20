package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.storage.PersistentStorage
import java.util.concurrent.ConcurrentHashMap

class InMemoryPersistentStorage : PersistentStorage {
    private val store = ConcurrentHashMap<String, String>()
    override fun store(data: MutableMap<String, String>) {
        store.clear()
        store.putAll(data)
    }

    override val all: Map<String, String>
        get() = store
}