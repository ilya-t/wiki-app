package com.tsourcecode.wiki.app

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PullToRefreshController(
        activity: AppCompatActivity,
        navigator: AppNavigator,
        projectComponentResolver: ProjectComponentResolver,
) {
    private val swipeRefreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.pull_to_refresh_container)
    private val contentContainer = activity.findViewById<View>(R.id.content_container)
    private var refreshJob: Job? = null

    init {
        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            canScrollUp(contentContainer)
        }

        activity.lifecycleScope.launch {
            navigator.data.collect { uri ->
                val component = projectComponentResolver.tryResolve(uri)
                swipeRefreshLayout.isEnabled = component != null
                refreshJob?.cancel()
                if (component != null) {
                    swipeRefreshLayout.setOnRefreshListener {
                        component.backendController.pullOrSync("pull-to-refresh")
                    }
                    refreshJob = launch {
                        component.backendController.refreshFlow.collect { refreshing ->
                            swipeRefreshLayout.isRefreshing = refreshing
                        }
                    }
                } else {
                    swipeRefreshLayout.setOnRefreshListener(null)
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun canScrollUp(view: View): Boolean {
        if (view.canScrollVertically(-1)) {
            return true
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (canScrollUp(view.getChildAt(i))) {
                    return true
                }
            }
        }
        return false
    }
}
