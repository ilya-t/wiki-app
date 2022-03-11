package com.tsourcecode.wiki.app.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.commitment.CommitScreenView
import com.tsourcecode.wiki.app.config.ConfigScreenView
import com.tsourcecode.wiki.app.documents.FileManagerView
import com.tsourcecode.wiki.app.editor.EditorScreenView
import com.tsourcecode.wiki.lib.domain.DomainComponent

class ScreenFactory(
        private val activity: AppCompatActivity,
        private val domainComponent: DomainComponent,
) {
    fun configScreen(): ScreenView {
        return ConfigScreenView(
                activity,
                domainComponent.configScreenModel,
                activity.lifecycleScope,
        )
    }

    fun fileManager(): ScreenView {
        return FileManagerView(
                domainComponent.projectComponents,
                activity,
                domainComponent.docContentProvider,
                domainComponent.projectsRepository,
                domainComponent.fileManagerModel,
        )
    }

    fun documentEditor(): ScreenView {
        return EditorScreenView(
                activity,
                domainComponent.docContentProvider,
                domainComponent.projectsRepository,
                domainComponent.projectComponents,
        )
    }

    fun changes(): ScreenView {
        return CommitScreenView(
                activity,
                activity.lifecycleScope,
                domainComponent.projectsRepository,
                domainComponent.projectComponents,
        )
    }
}