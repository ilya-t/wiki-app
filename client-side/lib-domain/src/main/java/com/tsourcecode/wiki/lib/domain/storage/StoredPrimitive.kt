package com.tsourcecode.wiki.lib.domain.storage

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
