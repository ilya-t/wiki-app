package com.tsourcecode.wiki.lib.domain

import java.io.File
import java.util.*

class JdkPlatformDeps : PlatformDeps {
    override val filesDir: File
        get() = File("/tmp/wiki.test.${UUID.randomUUID()}").apply {
            mkdirs()
        }
    override val persistentStorageProvider = InMemoryStorageProvider()
}
