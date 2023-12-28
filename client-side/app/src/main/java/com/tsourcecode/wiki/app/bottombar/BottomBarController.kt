package com.tsourcecode.wiki.app.bottombar

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.launch
import java.net.URI

class BottomBarController(
        private val activity: AppCompatActivity,
        private val navigator: AppNavigator,
        private val projectComponentResolver: ProjectComponentResolver,
        private val rootView: View,
) {
    private val scope: CoroutineScope = activity.lifecycleScope
    private val btnHome = rootView.findViewById<View>(R.id.btn_home)
    private val btnSync = rootView.findViewById<View>(R.id.btn_sync)

    private val btnCommit = rootView.findViewById<AppCompatButton>(R.id.btn_commit)

    init {
        scope.launch {
            navigator.data.collect { uri ->
                val projectComponent = projectComponentResolver.tryResolve(uri)
                bindCommitButton(projectComponent)
                bindFilesButton(projectComponent)
                bindSyncButton(projectComponent)
                rootView.visibility = if (projectComponent == null) View.GONE else View.VISIBLE
            }
        }
    }

    private fun bindFilesButton(projectComponent: ProjectComponent?) {
        btnHome.setOnClickListener {
            navigator.open(AppNavigator.PROJECTS_URI)
        }
    }

    private fun bindCommitButton(projectComponent: ProjectComponent?) {
        if (projectComponent == null) {
            btnCommit.text = "status"
            btnCommit.setOnClickListener(null)
            return
        }

        btnCommit.text = "status"
        scope.launch {
            suspend fun diffCount() = projectComponent
                .statusModel
                .statusFlow
                .last()
                .items.filterIsInstance<StatusViewItem.FileViewItem>()
                .size

            val diffCount = diffCount()
            if (diffCount > 0) {
                btnCommit.text = "status ($diffCount)"
            } else {
                btnCommit.text = "status"
            }

            btnCommit.setOnClickListener {
                if (!AppNavigator.isChanges(navigator.data.value)) {
                    navigator.open(URI("settings://changes/${projectComponent.project.name}"))
                } else if (diffCount > 0){
                    Snackbar.make(activity.findViewById(R.id.content_container), "Ready to commit changes and push?", 4000).apply {
                        setAction("YES!") {
                            projectComponent.statusModel.commit()
                        }
                        show()
                    }
                }
            }
        }
    }

    private fun bindSyncButton(component: ProjectComponent?) {
        btnSync.setOnClickListener {
            component?.backendController?.pullAndSync()
        }
    }
}
