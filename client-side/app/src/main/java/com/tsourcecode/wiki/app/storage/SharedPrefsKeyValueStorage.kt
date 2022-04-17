package com.tsourcecode.wiki.app.storage

import android.content.SharedPreferences
import com.tsourcecode.wiki.lib.domain.storage.KeyValueStorage

class SharedPrefsKeyValueStorage(
    private val sharedPreferences: SharedPreferences,

) : KeyValueStorage {
    override fun get(key: String): String? {
        return sharedPreferences.getString(key, null)
    }

    override fun set(key: String, value: String?) {
        sharedPreferences
            .edit()
            .putString(key, value)
            .apply()
    }

    override fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }
}