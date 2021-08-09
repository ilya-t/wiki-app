package com.tsourcecode.wiki.app.navigation

import android.view.View
import java.io.Closeable


class ScreenBootstrapper(
        private val screen: Screen,
        private val activityNavigator: ActivityNavigator,
        private val bootPoint: (View) -> Closeable?
) {
    private var openedResource: Closeable? = null
    init {
        activityNavigator.data.observeForever {
            openedResource?.close()
            openedResource = null
            if (it.screen == screen) {
                openedResource = bootPoint(it.layout)
            }
        }
    }
}