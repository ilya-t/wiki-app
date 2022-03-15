package com.tsourcecode.wiki.lib.domain.navigation

import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.PlatformDeps
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URI

private const val PREF_KEY = "project_name"

class InitialNavigationController(
    scope: CoroutineScope,
    platformDeps: PlatformDeps,
    navigator: AppNavigator,
    projectComponentResolver: ProjectComponentResolver,
) {
    private val storage = platformDeps.persistentStorageProvider.get("default_project")
    private val defaultProjectName: String? = storage.all[PREF_KEY]
    init {
        defaultProjectName?.let {
            navigator.open(URI("open://$defaultProjectName/"))
        }

        scope.launch {
            navigator.data.collect {
                if (AppNavigator.isFileManagerNavigation(it)) {
                    val component = projectComponentResolver.tryResolve(it) ?: return@collect
                    //TODO: project.id would be more stable
                    storage.store(mutableMapOf(PREF_KEY to component.project.name))
                }
            }
        }
    }
}
