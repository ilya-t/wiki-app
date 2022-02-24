package com.tsourcecode.wiki.app.documents

import android.view.ViewGroup
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.app.navigation.ScreenBootstrapper
import com.tsourcecode.wiki.lib.domain.documents.ActiveDocumentController
import com.tsourcecode.wiki.lib.domain.documents.DocumentContentProvider
import com.tsourcecode.wiki.lib.domain.documents.FileManagerModel

class FileManagerScreenController(
        private val navigator: ActivityNavigator,
        private val model: FileManagerModel,
        docContentProvider: DocumentContentProvider,
        private val activeDocumentController: ActiveDocumentController,
) {
    init {
        ScreenBootstrapper(
                Screen.FILE_MANAGER,
                navigator,
                bootPoint = {
                    FileManagerRecyclerController(
                            container = it as ViewGroup,
                            model.dataFlow,
                            openDelegate = this::openElement,
                            docContentProvider,
                    )
                }
        )
    }

    private fun openElement(e: Element) {
        when (e) {
            is Document -> {
                activeDocumentController.switch(e)
                navigator.open(Screen.DOCUMENT)
            }
            is Folder -> model.navigateTo(e)
        }
    }

    fun navigateBackward() = model.navigateBackward()
}
