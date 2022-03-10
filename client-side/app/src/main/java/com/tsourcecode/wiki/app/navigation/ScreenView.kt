package com.tsourcecode.wiki.app.navigation

import android.view.View
import java.io.Closeable

interface ScreenView : Closeable {
    val view: View
}