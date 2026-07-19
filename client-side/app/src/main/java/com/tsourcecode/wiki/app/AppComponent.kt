package com.tsourcecode.wiki.app

import android.content.Context
import com.tsourcecode.wiki.lib.domain.DomainComponent

class AppComponent(private val appContext: Context) {
    internal val domain = DomainComponent(
        AndroidPlatformDeps(appContext),
    )
    val quickStatusController = domain.quickStatusController
    val androidTaskScheduler = domain.platformDeps.taskScheduler

    init {
        if (BuildConfig.DEBUG) {
            domain.debugLogger.impl = { android.util.Log.d("_debug_", it) }
        }
    }

    companion object {
        private var appContext: Context? = null
        fun attachContext(c: Context) {
            appContext = c
        }

        val INSTANCE: AppComponent by lazy {
            AppComponent(appContext!!)
        }
    }
}