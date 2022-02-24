package com.tsourcecode.wiki.app

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.snackbar.Snackbar
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem

class EditStateController(
        private val activity: AppCompatActivity,
        private val navigator: ActivityNavigator,
        private val statusModel: StatusModel,
) {
    private val btnCommit = activity.findViewById<AppCompatButton>(R.id.btn_commit).apply {
        setOnClickListener {
            if (navigator.currentScreen != Screen.COMMIT) {
                navigator.open(Screen.COMMIT)
            } else {
                Snackbar.make(activity.findViewById(R.id.content_container), "Ready to commit changes and push?", 4000).apply {
                    setAction("YES!") {
                            statusModel.commit()
                    }
                    show()
                }

            }
        }
    }

    init {
        navigator.data.observe(activity) {
            if (it.screen == Screen.COMMIT) {
                btnCommit.text = "commit"
            } else {
                val diffCount = statusModel.statusFlow.value
                        .items.filterIsInstance<StatusViewItem.FileViewItem>()
                        .size
                if (diffCount > 0) {
                    btnCommit.text = "changes ($diffCount)"
                } else {
                    btnCommit.text = "changes"
                }
            }
        }
    }
}
