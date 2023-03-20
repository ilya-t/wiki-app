package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorage
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider

class InMemoryStorageProvider : PersistentStorageProvider {
    private val persistentStorages = mutableMapOf<String, PersistentStorage>()
    private val keyValueStorages = mutableMapOf<String, KeyValueStorage>()

    override fun get(name: String): PersistentStorage = persistentStorages.getOrPut(name) {
        InMemoryPersistentStorage()
    }

    override fun getKeyValueStorage(name: String): KeyValueStorage = keyValueStorages.getOrPut(name) {
        InMemoryKeyValueStorage()
    }
}