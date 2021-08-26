package com.tsourcecode.wiki.lib.domain.storage

interface PersistentStorageProvider {
    fun get(name: String): PersistentStorage
}