package com.tsourcecode.wiki.lib.domain.navigation

import com.tsourcecode.wiki.lib.domain.AppNavigator
import java.net.URI

enum class NavigationScreen {
    FILE_MANAGER,
    EDITOR,
    CHANGES,
    SEARCH,
    PROJECTS,
    ;

    companion object {
        fun resolveScreen(uri: URI): NavigationScreen? {
            if (uri == AppNavigator.PROJECTS_URI) {
                return PROJECTS
            }
            if (AppNavigator.isFileManagerNavigation(uri)) {
                return null
            }
            if (AppNavigator.isDocumentEdit(uri)) {
                return null
            }
            if (AppNavigator.isChanges(uri)) {
                return CHANGES
            }

            if (AppNavigator.isSearch(uri)) {
                return null
            }

            return null
        }
    }
}
