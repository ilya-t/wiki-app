package com.tsourcecode.wiki.app

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URI

class EditStateController(
        private val activity: AppCompatActivity,
        private val navigator: AppNavigator,
        private val statusModel: StatusModel,
        private val project: Project,
) {
    private val btnCommit = activity.findViewById<AppCompatButton>(R.id.btn_commit).apply {
        setOnClickListener {
            if (!AppNavigator.isChanges(navigator.data.value)) {
                navigator.open(URI("settings://changes/${project.name}"))
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
        activity.lifecycleScope.launch {
            navigator.data.collect { uri ->
                if (AppNavigator.isChanges(uri)) {
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
}
