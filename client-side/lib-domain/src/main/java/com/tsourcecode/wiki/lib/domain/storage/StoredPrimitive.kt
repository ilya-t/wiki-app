package com.tsourcecode.wiki.lib.domain.storage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray

interface TypeConverter<T> {
    fun parse(s: String?): T?
    fun toString(t: T): String
}

class StoredPrimitive<T>(
    private val typeConverter: TypeConverter<T>,
    private val key: String,
    private val keyValueStorage: KeyValueStorage,
) {
    var value: T? = typeConverter.parse(keyValueStorage.get(key))
        set(value) {
            field = value

            if (value == null) {
                keyValueStorage[key] = null
            } else {
                keyValueStorage[key] = typeConverter.toString(value)
            }
        }

    companion object {
        fun string(key:String, storageProvider: KeyValueStorage): StoredPrimitive<String> {
            return StoredPrimitive(
                object : TypeConverter<String> {
                    override fun parse(s: String?) = s

                    override fun toString(t: String) = t
                },
                key,
                storageProvider
            )
        }

        fun stringList(key:String, storageProvider: KeyValueStorage): StoredPrimitive<List<String>> {
            return StoredPrimitive(
                object : TypeConverter<List<String>> {
                    override fun parse(s: String?): List<String> {
                        if (s == null) {
                            return emptyList()
                        }

                        return Json.parseToJsonElement(s).jsonArray.map { it.toString() }
                    }

                    override fun toString(t: List<String>): String {
                        return JsonArray(t.map { JsonPrimitive((it)) }).toString()
                    }
                },
                key,
                storageProvider
            )
        }

        fun long(key:String, storageProvider: KeyValueStorage): StoredPrimitive<Long> {
            return StoredPrimitive(
                object : TypeConverter<Long> {
                    override fun parse(s: String?): Long? {
                        return try {
                            s?.toLong()
                        } catch (e: NumberFormatException) {
                            //Assert.fail(e)
                            //return null
                            throw e
                        }
                    }

                    override fun toString(t: Long) = t.toString()
                },
                key,
                storageProvider
            )
        }
    }
}
