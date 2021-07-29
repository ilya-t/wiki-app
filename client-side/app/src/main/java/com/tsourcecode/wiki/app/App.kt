package com.tsourcecode.wiki.app

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppComponent.attachContext(this)
    }
}