package com.tsourcecode.wiki.lib.domain.storage

interface PersistentStorage {
    fun store(data: MutableMap<String, String>)

    val all: Map<String, String>
}