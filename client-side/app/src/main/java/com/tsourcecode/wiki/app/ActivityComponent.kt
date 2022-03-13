package com.tsourcecode.wiki.app

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.ScreenFactory
import com.tsourcecode.wiki.app.search.SearchScreenController
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URI

class ActivityComponent(
        private val activity: AppCompatActivity,
        private val appComponent: AppComponent,
) {
    private val screenFactory = ScreenFactory(
            activity,
            appComponent.domain,
    )
    private val navigator = ActivityNavigator(
            activity,
            appComponent.domain.navigator,
            screenFactory,
    ).apply {
        val btnFiles = activity.findViewById<View>(R.id.btn_files)
        btnFiles.setOnClickListener {
            val name = appComponent.domain.projectsRepository.data.value.first().name
            appComponent.domain.navigator.open(URI("open://$name/"))
        }
    }

    init {
        activity.lifecycleScope.launch {
            appComponent.domain.projectsRepository.data.collect {
                it.firstOrNull()?.let { project ->
                    bootProjectDeps(project)
                }
            }
        }
    }

    private fun bootProjectDeps(p: Project) {
        val component = appComponent.domain.projectComponents.get(p)
        val editStateController = EditStateController(
                activity,
                appComponent.domain.navigator,
                component.statusModel,
                p,
        )
    }
    private val searchScreenController = SearchScreenController(
            appComponent.domain.navigator,
            activity,
            appComponent.domain.projectsRepository,
    )

    private val quickStateController = QuickStatusViewModel(
            activity,
            appComponent.quickStatusController)

    fun dispatchBackPressed(): Boolean {
        if (appComponent.domain.navigator.goBack()) {
            return true
        }

        return false
    }
}