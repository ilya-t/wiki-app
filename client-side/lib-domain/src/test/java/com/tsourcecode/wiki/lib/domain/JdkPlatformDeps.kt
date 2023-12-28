package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.storage.ExternalStorageAccessHandler
import com.tsourcecode.wiki.lib.domain.util.JdkThreading
import java.io.File
import java.util.UUID

class JdkPlatformDeps : PlatformDeps {
    override val persistentStorageProvider = InMemoryStorageProvider()
    override val threading = JdkThreading()
    override val externalStorageAccess = object : ExternalStorageAccessHandler {
        override val accessGranted = true

        override fun requestAccess() = Unit
    }

    override val internalFiles: File
        get() {
            return File("/tmp/wiki.test.${UUID.randomUUID()}").apply {
                mkdirs()
            }
        }

    override suspend fun filesDir(): File {
        return File("/tmp/wiki.test.${UUID.randomUUID()}").apply {
            mkdirs()
        }
    }
}
