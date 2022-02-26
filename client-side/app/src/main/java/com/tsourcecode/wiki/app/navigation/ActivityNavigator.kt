package com.tsourcecode.wiki.app.navigation

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tsourcecode.wiki.app.R

class ActivityNavigator(private val activity: AppCompatActivity) {
    private val contentContainer: FrameLayout
    var currentScreen: Screen = Screen.FILE_MANAGER
        private set
    private val _data = MutableLiveData<ScreenDetails>()
    val data: LiveData<ScreenDetails> = _data

    init {
        activity.setContentView(R.layout.main_layout)
        contentContainer = activity.findViewById(R.id.content_container)
        openInternal(currentScreen)
    }

    fun open(screen: Screen) {
        if (currentScreen == screen) {
            return
        }
        currentScreen = screen
        openInternal(screen)
    }

    private fun openInternal(screen: Screen) {
        contentContainer.removeAllViews()
        _data.value = ScreenDetails(
                screen,
                screen.inflateLayout(),
        )
    }

    private fun Screen.inflateLayout(): View {
        val id = when (this) {
            Screen.CONFIG -> R.layout.config
            Screen.FILE_MANAGER -> R.layout.file_manager
            Screen.DOCUMENT -> R.layout.document_editor
            Screen.COMMIT -> R.layout.commit
            Screen.SEARCH -> R.layout.search
        }

        return LayoutInflater.from(activity).inflate(id, contentContainer)
    }
}
