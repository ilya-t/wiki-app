package com.tsourcecode.wiki.lib.domain.storage

interface KeyValueStorage {
    operator fun get(key: String): String?
    operator fun set(key: String, value: String?)
    operator fun contains(key: String): Boolean
}