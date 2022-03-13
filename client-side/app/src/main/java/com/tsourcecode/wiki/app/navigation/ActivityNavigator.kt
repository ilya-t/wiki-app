package com.tsourcecode.wiki.app.navigation

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.navigation.NavigationScreen
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URI

class ActivityNavigator(
        private val activity: AppCompatActivity,
        private val appNavigator: AppNavigator,
        private val screenFactory: ScreenFactory,
) {
    private var openedScreen: ScreenDetails? = null
    private val contentContainer: FrameLayout

    init {
        activity.setContentView(R.layout.main_layout)
        contentContainer = activity.findViewById(R.id.content_container)

        activity.lifecycleScope.launch {
            appNavigator.data.collect { uri ->
                val navigationScreen = resolveScreen(uri)
                if (openedScreen?.screen == navigationScreen &&
                        openedScreen?.screenView?.handle(uri) == true) {
                    return@collect
                }
                openedScreen?.screenView?.close()
                contentContainer.removeAllViews()
                val newScreen = ScreenDetails(
                        navigationScreen,
                        screenFactory.create(navigationScreen)
                )

                contentContainer.addView(newScreen.screenView.view)
                if (!newScreen.screenView.handle(uri)) {
                    throw RuntimeException("Screen '$newScreen' failed handling of: '$uri'")
                }
                openedScreen = newScreen
            }
        }
    }

    private fun resolveScreen(uri: URI): NavigationScreen {
        return NavigationScreen.resolveScreen(uri) ?: throw RuntimeException("No-one can handle: $uri")
    }
}

private class ScreenDetails(
        val screen: NavigationScreen,
        val screenView: ScreenView,
)