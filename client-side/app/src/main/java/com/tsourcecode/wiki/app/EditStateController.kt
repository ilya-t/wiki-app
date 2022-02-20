package com.tsourcecode.wiki.app

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen

class EditStateController(
        private val context: AppCompatActivity,
        private val navigator: ActivityNavigator,
) {
    private val btnCommit = context.findViewById<View>(R.id.btn_commit).apply {
        setOnClickListener {
            navigator.open(Screen.COMMIT)
        }
    }
}
