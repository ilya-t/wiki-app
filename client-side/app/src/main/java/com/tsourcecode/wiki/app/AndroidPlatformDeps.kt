package com.tsourcecode.wiki.app

import android.content.Context
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import java.io.File

class AndroidPlatformDeps(
        private val context: Context,
) : PlatformDeps {
    override val filesDir: File = context.filesDir
}
