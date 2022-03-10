package com.tsourcecode.wiki.app.navigation

import android.view.View
import java.io.Closeable
import java.net.URI

interface ScreenView : Closeable {
    val view: View
    fun handle(uri: URI): Boolean
}