package com.tsourcecode.wiki.app.bottombar

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.project.ProjectComponent
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import kotlinx.coroutines.CoroutineScope
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

    init {
        btnHome.setOnClickListener {
            navigator.open(AppNavigator.PROJECTS_URI)
        }
        scope.launch {
            navigator.data.collect { uri ->
                val projectComponent = projectComponentResolver.tryResolve(uri)
                bindSyncButton(projectComponent)
                rootView.visibility = if (projectComponent == null) View.GONE else View.VISIBLE
            }
        }
    }

    private fun bindSyncButton(component: ProjectComponent?) {
        btnPullOrSync.setOnClickListener {
            component?.backendController?.pullOrSync("sync-button")
        }
    }
}
