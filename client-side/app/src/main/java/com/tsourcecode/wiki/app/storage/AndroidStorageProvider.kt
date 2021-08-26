package com.tsourcecode.wiki.app.storage

import android.content.Context
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorage
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider

class AndroidStorageProvider(
        private val context: Context,
) : PersistentStorageProvider {
    override fun get(name: String): PersistentStorage {
        return SharedPrefsStorage(context.getSharedPreferences(name, Context.MODE_PRIVATE))
    }
}