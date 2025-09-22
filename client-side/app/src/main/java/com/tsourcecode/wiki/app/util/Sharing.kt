package com.tsourcecode.wiki.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

object Sharing {
    private const val AUTHORITY = "com.tsourcecode.wiki.files"

    fun shareTextAsFile(c: Context, text: String, fileName: String = "debug_log.md"): Result<Intent> {
        val file = File(c.filesDir, fileName)
        try {
            file.writeText(text)
        } catch (e: IOException) {
            return Result.failure(e)
        }

        return shareFile(c, file)
    }

    fun shareFile(c: Context, f: File): Result<Intent> {
        if (!f.absolutePath.startsWith(c.filesDir.absolutePath)) {
            return Result.failure(
                IllegalArgumentException("""
                File must be located at ${c.filesDir.absolutePath}. 
                Instead got: ${f.absolutePath}
            """.trimIndent())
            )
        }

        val contentUri: Uri = FileProvider.getUriForFile(c, AUTHORITY, f)

        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/*"
        sharingIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        return Result.success(Intent.createChooser(sharingIntent, f.name))
    }
}
