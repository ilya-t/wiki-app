package com.tsourcecode.wiki.app

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.tsourcecode.wiki.lib.domain.backend.BackendController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PullToRefreshTrigger(
        activity: AppCompatActivity,
        backendController: BackendController,
) {
    private val swipeRefreshLayout = activity.findViewById<SwipeRefreshLayout>(R.id.pull_to_refresh_container)

    init {
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
