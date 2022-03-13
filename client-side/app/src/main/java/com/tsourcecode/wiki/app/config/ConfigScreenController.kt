package com.tsourcecode.wiki.app.config

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.tsourcecode.wiki.app.navigation.ScreenView
import com.tsourcecode.wiki.lib.domain.AppNavigator.Companion.PROJECTS_URI
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenItem
import com.tsourcecode.wiki.lib.domain.config.ConfigScreenModel
import com.tsourcecode.wiki.lib.domain.config.SubmitButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URI

class ConfigScreenView(
        activity: AppCompatActivity,
        private val configModel: ConfigScreenModel,
        scope: CoroutineScope,
) : ScreenView {
    private val composeView = ComposeView(activity)
    override val view: View = composeView
    override fun handle(uri: URI) = uri == PROJECTS_URI

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
                    is ConfigScreenItem.EditableElement -> EditableElement(i, item)
                    is ConfigScreenItem.ImportFrom -> RenderImport(i, item)
                    is ConfigScreenItem.PreviewElement -> PreviewElement(i, item)
                    is ConfigScreenItem.PlusElement -> RenderPlusElement()
                }.apply { /* exhaustive */ }

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
                    onValueChange = { configModel.edit(item.copy(projectUrl = it)) },
            )
            Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { configModel.submitImport(item) }) {
                Text(text = "import")
            }

        }
    }

    @Composable
    private fun PreviewElement(index: Int, item: ConfigScreenItem.PreviewElement) {
        Row(modifier = Modifier
                .fillMaxWidth()
                .padding(Dp(8f))
                .border(
                        width = Dp(1f),
                        color = Color.Gray,
                        shape = RoundedCornerShape(CornerSize(Dp(4f))),
                ),
        ) {
            Text(
                    modifier = Modifier
                            .padding(Dp(8f))
                            .fillMaxWidth(.80f)
                            .align(CenterVertically)
                            .clickable {
                                configModel.open(item)
                            },
                    text = item.projectName,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18f.sp,
            )
            Button(
                    modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    colors = buttonColors(),
                    onClick = { configModel.edit(index, item) }) {
                Text(
                        text = "edit",
                )
            }        }
    }
    @Composable
    private fun EditableElement(index: Int, item: ConfigScreenItem.EditableElement) {
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
                    value = item.repoUrl,
                    onValueChange = { configModel.edit(index, item.copy(repoUrl = it)) },
            )

            Button(modifier = Modifier
                    .fillMaxWidth(),
                    colors = item.submitButton.colors(),
                    enabled = item.submitEnabled,
                    onClick = { configModel.submit(item) }) {
                Text(
                        text = "apply",
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

    @Composable
    fun RenderPlusElement() {
        Button(modifier = Modifier
                .fillMaxWidth(),
                colors = SubmitButton.ADD.colors(),
                onClick = { configModel.addNewElement() }) {
            Text(
                    text = "add",
            )
        }
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
