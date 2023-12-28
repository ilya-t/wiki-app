package com.tsourcecode.wiki.app

import android.content.Context
import android.os.Environment
import com.tsourcecode.wiki.app.storage.AndroidStorageProvider
import com.tsourcecode.wiki.app.storage.ExternalStorageAccessHandlerImpl
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.io.File

class AndroidPlatformDeps(
        private val context: Context,
) : PlatformDeps {
    private val filesDir = MutableStateFlow<File?>(null)
    override val externalStorageAccess = ExternalStorageAccessHandlerImpl(
        onAccessGranted = {
            val root = File(Environment.getExternalStorageDirectory().path + "/workspace")
            root.mkdirs()
            filesDir.value = root
        },
    )
    override suspend fun filesDir(): File {
        return filesDir.filterNotNull().first()
    }

    override val internalFiles: File = context.filesDir

    override val persistentStorageProvider = AndroidStorageProvider(context)
    override val threading = AndroidThreading()
}
