package com.tsourcecode.wiki.app

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.commitment.CommitScreenView
import com.tsourcecode.wiki.app.editor.EditorScreenController
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.app.navigation.ScreenFactory
import com.tsourcecode.wiki.app.search.SearchScreenController
import com.tsourcecode.wiki.lib.domain.project.Project
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
            open(Screen.FILE_MANAGER)
        }
        data.observeForever {
            btnFiles.isEnabled = it.screen != Screen.FILE_MANAGER
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

        navigator.open(Screen.CONFIG)
    }

    private fun bootProjectDeps(p: Project) {
        val component = appComponent.domain.projectComponents.get(p)
        val ptrTrigger = PullToRefreshTrigger(
                activity,
                component.backendController,
                navigator,
        )
        val editStateController = EditStateController(
                activity,
                navigator,
                component.statusModel,
        )

        val searchScreenController = SearchScreenController(
                navigator,
                activity,
                component.searchModel,
        )

        val commitScreenView = CommitScreenView(
                activity,
                navigator,
                component.statusModel,
        )
        val editorScreenController = EditorScreenController(
                activity,
                navigator,
                appComponent.docContentProvider,
                component.documentsController,
                appComponent.domain.activeDocumentController,
        )
    }

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