package com.tsourcecode.wiki.app.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.commitment.CommitScreenView
import com.tsourcecode.wiki.app.config.ConfigScreenView
import com.tsourcecode.wiki.app.documents.FileManagerView
import com.tsourcecode.wiki.app.editor.EditorScreenView
import com.tsourcecode.wiki.app.search.SearchScreenView
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
                activity,
                domainComponent.docContentProvider,
                domainComponent.projectComponentResolver,
                domainComponent.fileManagerModel,
        )
    }

    fun documentEditor(): ScreenView {
        return EditorScreenView(
                activity,
                domainComponent.projectComponentResolver,
                domainComponent.docContentProvider,
        )
    }

    fun changes(): ScreenView {
        return CommitScreenView(
                activity,
                activity.lifecycleScope,
                domainComponent.projectComponentResolver,
        )
    }

    fun search(): ScreenView {
        return SearchScreenView(
                activity,
                activity.lifecycleScope,
                domainComponent.projectComponentResolver,
        )
    }
}