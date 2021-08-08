package com.tsourcecode.wiki.app

import androidx.appcompat.app.AppCompatActivity

class ActivityComponent(
        private val activity: AppCompatActivity,
) {
    val quickStateController = QuickStatusViewModel(
            activity,
            AppComponent.INSTANCE.quickStatusController)
}