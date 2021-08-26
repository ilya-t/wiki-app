package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider
import java.io.File

interface PlatformDeps {
    val filesDir: File
    val persistentStorageProvider: PersistentStorageProvider
}