package com.tsourcecode.wiki.app.config

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.EditText
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.app.navigation.ScreenBootstrapper
import java.io.Closeable

class ConfigScreenController(
        activity: Activity,
        navigator: ActivityNavigator,
) {
    private val p = activity.getSharedPreferences("tmp", Context.MODE_PRIVATE)

    init {
        ScreenBootstrapper(
                Screen.CONFIG,
                navigator,
                bootPoint = {
                    ConfigScreenView(it)
                }
        )
    }
}

private class ConfigScreenView(v: View) : Closeable {
    private val hostInputField = v.findViewById<EditText>(R.id.edit_text_host)

    init {
//        hostInputField.addTextChangedListener()
    }

    override fun close() {

    }

}
