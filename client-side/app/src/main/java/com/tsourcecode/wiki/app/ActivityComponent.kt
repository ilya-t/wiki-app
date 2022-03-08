package com.tsourcecode.wiki.app

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tsourcecode.wiki.app.commitment.CommitScreenView
import com.tsourcecode.wiki.app.config.ConfigScreenController
import com.tsourcecode.wiki.app.documents.FileManagerScreenController
import com.tsourcecode.wiki.app.editor.EditorScreenController
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.app.search.SearchScreenController

class ActivityComponent(
        private val activity: AppCompatActivity,
        private val appComponent: AppComponent,
) {
    private val defaultProject = appComponent.domain.defaultProjectComponent

    private val navigator = ActivityNavigator(activity).apply {
        val btnFiles = activity.findViewById<View>(R.id.btn_files)
        btnFiles.setOnClickListener {
            open(Screen.FILE_MANAGER)
        }
        data.observeForever {
            btnFiles.isEnabled = it.screen != Screen.FILE_MANAGER
        }
    }

    private val ptrTrigger = PullToRefreshTrigger(
            activity,
            defaultProject.backendController,
            navigator,
    )
    private val quickStateController = QuickStatusViewModel(
            activity,
            appComponent.quickStatusController)

    private val editStateController = EditStateController(
            activity,
            navigator,
            defaultProject.statusModel,
    )

    private val searchScreenController = SearchScreenController(
            navigator,
            activity,
            defaultProject.searchModel,
    )

    private val commitScreenView = CommitScreenView(
            activity,
            navigator,
            defaultProject.statusModel,
    )
    private val editorScreenController = EditorScreenController(
            activity,
            navigator,
            appComponent.docContentProvider,
            defaultProject.documentsController,
            appComponent.domain.activeDocumentController,
    )

    private val fileManagerScreenController = FileManagerScreenController(
            defaultProject.project,
            activity,
            navigator,
            defaultProject.fileManagerModel,
            appComponent.docContentProvider,
            appComponent.domain.activeDocumentController,
    )

    private val configScreenController = ConfigScreenController(
            activity,
            navigator,
            appComponent.domain.configScreenModel,
    )

    fun dispatchBackPressed(): Boolean {
        if (navigator.currentScreen != Screen.FILE_MANAGER) {
            navigator.open(Screen.FILE_MANAGER)
            return true
        }

        if (fileManagerScreenController.navigateBackward()) {
            return true
        }

        return false
    }
}