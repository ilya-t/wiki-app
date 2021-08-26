package com.tsourcecode.wiki.app.storage

import android.content.SharedPreferences
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorage

class SharedPrefsStorage(
        private val sharedPreferences: SharedPreferences,
) : PersistentStorage {
    override fun store(data: MutableMap<String, String>) {
        val editor = sharedPreferences.edit()

        data.forEach { (key, value) ->
            editor.putString(key, value)
        }

        editor.apply()
    }

    override val all: Map<String, String>
        get() = sharedPreferences.all as Map<String, String>
}