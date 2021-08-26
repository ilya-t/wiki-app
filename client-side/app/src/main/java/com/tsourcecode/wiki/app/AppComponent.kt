package com.tsourcecode.wiki.app

import android.content.Context
import com.tsourcecode.wiki.lib.domain.DomainComponent
import com.tsourcecode.wiki.lib.domain.documents.staging.ChangedFilesController

class AppComponent(private val appContext: Context) {
    private val domain = DomainComponent(AndroidPlatformDeps(appContext))
    val changedFilesController = domain.changedFilesController
    val backendController = domain.backendController
    val quickStatusController = domain.quickStatusController
    val docContentProvider = domain.docContentProvider

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