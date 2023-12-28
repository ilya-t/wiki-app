package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.storage.ExternalStorageAccessHandler
import com.tsourcecode.wiki.lib.domain.storage.PersistentStorageProvider
import com.tsourcecode.wiki.lib.domain.util.Threading
import java.io.File

interface PlatformDeps {
    val persistentStorageProvider: PersistentStorageProvider
    val threading: Threading
    val externalStorageAccess: ExternalStorageAccessHandler
    suspend fun filesDir(): File
    val internalFiles: File
}