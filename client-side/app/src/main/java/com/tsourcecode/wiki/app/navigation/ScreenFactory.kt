package com.tsourcecode.wiki.app.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.commitment.CommitScreenView
import com.tsourcecode.wiki.app.config.ConfigScreenView
import com.tsourcecode.wiki.app.documents.FileManagerView
import com.tsourcecode.wiki.app.editor.EditorScreenView
import com.tsourcecode.wiki.app.search.SearchScreenView
import com.tsourcecode.wiki.lib.domain.DomainComponent
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.navigation.NavigationScreen

class ScreenFactory(
        private val activity: AppCompatActivity,
        private val domainComponent: DomainComponent<out PlatformDeps>,
) {
    fun create(screen: NavigationScreen): ScreenView {
        return when (screen) {
            NavigationScreen.PROJECTS -> configScreen()
            NavigationScreen.FILE_MANAGER -> fileManager()
            NavigationScreen.EDITOR -> documentEditor()
            NavigationScreen.CHANGES -> changes()
            NavigationScreen.SEARCH -> search()
        }
    }

    private fun configScreen(): ScreenView {
        return ConfigScreenView(
                activity,
                domainComponent.viewModels.configScreenModel,
                activity.lifecycleScope,
        )
    }

    private fun fileManager(): ScreenView {
        return FileManagerView(
                activity,
                domainComponent.projectComponentResolver,
                domainComponent.fileManagerModel,
        )
    }

    private fun documentEditor(): ScreenView {
        return EditorScreenView(
                activity,
                domainComponent.viewModels.documentViewModelResolver,
        )
    }

    private fun changes(): ScreenView {
        return CommitScreenView(
                activity,
                activity.lifecycleScope,
                domainComponent.projectComponentResolver,
        )
    }

    private fun search(): ScreenView {
        return SearchScreenView(
                activity,
                activity.lifecycleScope,
                domainComponent.projectComponentResolver,
        )
    }
}