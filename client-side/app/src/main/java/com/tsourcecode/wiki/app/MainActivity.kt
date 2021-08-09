package com.tsourcecode.wiki.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var activityComponent: ActivityComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent = ActivityComponent(this, AppComponent.INSTANCE)
    }

    override fun onBackPressed() {
        if (activityComponent.dispatchBackPressed()) {
            return
        }

        super.onBackPressed()
    }
}