package com.tsourcecode.wiki.app.navigation

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.AppNavigator
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URI

class ActivityNavigator(
        private val activity: AppCompatActivity,
        private val appNavigator: AppNavigator,
        private val screenFactory: ScreenFactory,
) {
    private var openedScreen: ScreenView? = null
    private val contentContainer: FrameLayout
    var currentScreen: Screen = Screen.FILE_MANAGER
        private set
    private val _data = MutableLiveData<ScreenDetails>()
    val data: LiveData<ScreenDetails> = _data

    init {
        activity.setContentView(R.layout.main_layout)
        contentContainer = activity.findViewById(R.id.content_container)

        activity.lifecycleScope.launch {
            appNavigator.data.collect { uri ->
                if (openedScreen?.handle(uri) == true) {
                    return@collect
                }
                openedScreen?.close()
                contentContainer.removeAllViews()
                val newScreen = resolveScreen(uri)
                newScreen?.view?.let { v -> contentContainer.addView(v) }
                newScreen?.handle(uri)
                openedScreen = newScreen
            }
        }
    }

    private fun resolveScreen(uri: URI): ScreenView? {
        if (AppNavigator.isConfigUri(uri)) {
            return screenFactory.configScreen()
        }
        if (AppNavigator.isFileManagerNavigation(uri)) {
            return screenFactory.fileManager()
        }
        if (AppNavigator.isDocumentEdit(uri)) {
            return screenFactory.documentEditor()
        }
        return null
    }

    fun open(screen: Screen) = Unit

    private fun Screen.inflateLayout() {
        when (this) {
            Screen.FILE_MANAGER -> R.layout.file_manager
            Screen.DOCUMENT -> R.layout.document_editor
            Screen.COMMIT -> R.layout.commit
            Screen.SEARCH -> R.layout.search
        }
    }
}
