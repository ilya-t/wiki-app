package com.tsourcecode.wiki.app

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PullToRefreshTrigger(
        activity: AppCompatActivity,
        backendController: BackendController,
        navigator: ActivityNavigator,
) {
    private val swipeRefreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.pull_to_refresh_container)

    init {
        navigator.data.observe(activity) {
            swipeRefreshLayout.isEnabled = it.screen == Screen.FILE_MANAGER
        }

        swipeRefreshLayout.setOnRefreshListener {
            backendController.sync()
        }
        activity.lifecycleScope.launch {
            backendController.refreshFlow.collect { refreshing ->
                withContext(Dispatchers.Main) {
                    swipeRefreshLayout.isRefreshing = refreshing
                }
            }
        }
    }

}
