package com.tsourcecode.wiki.app.config

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.app.navigation.ScreenBootstrapper
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.config.SubmitButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.Closeable

class ConfigScreenController(
        activity: AppCompatActivity,
        navigator: ActivityNavigator,
        configModel: ConfigScreenModel,
) {
    init {
        ScreenBootstrapper(
                Screen.CONFIG,
                navigator,
                bootPoint = {
                    ConfigScreenView(it, configModel, activity.lifecycleScope)
                }
        )
    }
}

private class ConfigScreenView(
        v: View,
        private val configModel: ConfigScreenModel,
        scope: CoroutineScope,
) : Closeable {
    private val composeView = v.findViewById<ComposeView>(R.id.config_compose_view)
    private val collector = scope.launch {
        configModel.data.collect {
            composeView.setContent { Compose(it) }
        }
    }

    @Composable
    private fun Compose(items: List<ConfigScreenItem>) {
        LazyColumn {
            items(items.size) { i ->
                when (val item = items[i]) {
                    is ConfigScreenItem.ConfigElement -> RenderElement(i, item)
                    is ConfigScreenItem.ImportFrom -> RenderImport(i, item)
                }

            }
        }
    }

    @Composable
    private fun RenderImport(index: Int, item: ConfigScreenItem.ImportFrom) {
        Column(modifier = Modifier
                .fillMaxWidth()
                .padding(Dp(8f))
        ) {
            InputField(
                    label = "import all projects from",
                    value = item.projectUrl,
                    onValueChange = { configModel.edit(index, item.copy(projectUrl = it)) },
            )

        }
    }

    @Composable
    private fun RenderElement(index: Int, item: ConfigScreenItem.ConfigElement) {
        Column(modifier = Modifier
                .fillMaxWidth()
                .padding(Dp(8f))
        ) {
            InputField(
                    label = "projects server address",
                    value = item.serverAddress,
                    onValueChange = { configModel.edit(index, item.copy(serverAddress = it)) },
            )
            InputField(
                    label = "name",
                    value = item.projectName,
                    onValueChange = { configModel.edit(index, item.copy(projectName = it)) },
            )

            InputField(
                    label = "repo url",
                    value = item.projectUrl,
                    onValueChange = { configModel.edit(index, item.copy(projectUrl = it)) },
            )

            Button(modifier = Modifier
                    .fillMaxWidth(),
                    colors = item.submitButton.colors(),
                    enabled = item.submitEnabled,
                    onClick = { configModel.submit(item) }) {
                Text(
                        text = item.submitButton.name,
                )
            }
        }
    }

    @Composable
    fun InputField(
            label: String,
            value: String,
            onValueChange: (String) -> Unit,
    ) {
        TextField(
                modifier = Modifier.fillMaxWidth(),
                value = value,
                onValueChange = onValueChange,
                label = {
                    Text(
                            text = label,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                ),
        )
    }

    override fun close() {
        collector.cancel()
    }

}

@Composable
private fun SubmitButton.colors(): ButtonColors {
    return when (this) {
        SubmitButton.APPLY -> buttonColors(
                backgroundColor = Color(98, 0, 238),
                contentColor = Color.White,
        )

        SubmitButton.ADD -> buttonColors(
                backgroundColor = Color(0, 200, 83, 255),
                contentColor = Color.White,
        )
    }
}
