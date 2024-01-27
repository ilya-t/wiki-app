package com.tsourcecode.wiki.app.bottombar

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BottomBarController(
        private val activity: AppCompatActivity,
        private val navigator: AppNavigator,
        private val projectComponentResolver: ProjectComponentResolver,
        private val rootView: View,
) {
    private val scope: CoroutineScope = activity.lifecycleScope
    private val btnHome = rootView.findViewById<View>(R.id.btn_home)
    private val btnPullOrSync = rootView.findViewById<AppCompatButton>(R.id.btn_pull)

    private val btnCommit = rootView.findViewById<AppCompatButton>(R.id.btn_commit)

    init {
        btnHome.setOnClickListener {
            navigator.open(AppNavigator.PROJECTS_URI)
        }
        scope.launch {
            navigator.data.collect { uri ->
                val projectComponent = projectComponentResolver.tryResolve(uri)
                bindCommitButton(projectComponent)
                bindSyncButton(projectComponent)
                syncButtonJob?.cancel()
                syncButtonJob = scope.launch(Dispatchers.Main) {
                    projectComponent?.fileStatusProvider?.statusFlow?.collect { status ->
                        btnPullOrSync.text = if (status == null || status.files.isEmpty()) {
                            "pull"
                        } else {
                            "sync"
                        }
                        btnCommit.isEnabled = status != null && status.files.isNotEmpty()
                    }
                }

                rootView.visibility = if (projectComponent == null) View.GONE else View.VISIBLE
            }
        }
    }

    private fun bindCommitButton(projectComponent: ProjectComponent?) {
        if (projectComponent == null) {
            btnCommit.isEnabled = false
            btnCommit.setOnClickListener(null)
            return
        }

        btnCommit.setOnClickListener {
            onCommitClick(projectComponent)
        }
    }

    private fun onCommitClick(projectComponent: ProjectComponent) {
        fun diffCount() = projectComponent
            .fileStatusProvider
            .statusFlow
            .value
            ?.files
            ?.size ?: 0

        scope.launch(Dispatchers.Main) {
            val diffCount = diffCount()
            if (diffCount > 0){
                Snackbar.make(activity.findViewById(R.id.content_container), "Ready to commit changes and push?", 4000).apply {
                    setAction("YES!") {
                        projectComponent.statusModel.commit()
                    }
                    show()
                }
            } else {
                Snackbar
                    .make(
                        activity.findViewById(R.id.content_container),
                        "Nothing to commit", 1000)
                    .show()
            }
        }
    }

    private var syncButtonJob: Job? = null

    private fun bindSyncButton(component: ProjectComponent?) {
        btnPullOrSync.setOnClickListener {
            component?.backendController?.pullOrSync()
        }
    }
}
