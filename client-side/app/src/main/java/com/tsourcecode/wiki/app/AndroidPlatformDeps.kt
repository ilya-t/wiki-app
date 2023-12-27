package com.tsourcecode.wiki.app

import android.content.Context
import com.tsourcecode.wiki.app.storage.AndroidStorageProvider
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

class AndroidPlatformDeps(
        private val context: Context,
) : PlatformDeps {
    override suspend fun filesDir(): File {
        return suspendCancellableCoroutine { cont ->
            val value: File = context.filesDir
            cont.resume(value)
        }
    }

    override val persistentStorageProvider = AndroidStorageProvider(context)
    override val threading = AndroidThreading()
}
