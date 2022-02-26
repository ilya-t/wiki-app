package com.tsourcecode.wiki.app.commitment

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults.textFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.lib.domain.commitment.FileStatus
import com.tsourcecode.wiki.lib.domain.commitment.Status
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CommitScreenView(
        private val lifecycle: LifecycleOwner,
        private val navigator: ActivityNavigator,
        private val statusModel: StatusModel,
) {
    init {
        navigator.data.observe(lifecycle) {
            if (it.screen == Screen.COMMIT) {
                statusModel.notifyCommitScreenOpened()
                setupScreen(it.layout)
            }
        }
    }

    private fun setupScreen(root: View) {
        val composeView = root.findViewById<ComposeView>(R.id.commit_container)
        lifecycle.lifecycleScope.launch {
            statusModel.statusFlow.collect { viewModel ->
                composeView.setContent {
                    ComposeCommitScreen(viewModel, statusModel)
                }
            }
        }
    }
}

@Composable
private fun ComposeCommitScreen(viewModel: StatusViewModel, model: StatusModel) {
    LazyColumn {
        items(viewModel.items) { item ->
            when (item) {
                is StatusViewItem.CommitViewItem -> CommitItem(item, model)
                is StatusViewItem.FileViewItem -> FileDiffItem(item, model)
            }.apply { /*exhaustive*/ }
        }
    }
}

@Composable
fun CommitItem(item: StatusViewItem.CommitViewItem, model: StatusModel) {
    Column(
            modifier = Modifier
                    .padding(Dp(8f))
    ) {
        TextField(
                modifier = Modifier
                        .fillMaxWidth()
                        .border(
                                width = Dp(1f),
                                color = Color.Gray,
                                shape = RoundedCornerShape(CornerSize(Dp(4f))),
                        ),
                value = item.commitMessage,
                onValueChange = { model.updateCommitText(it) },
                label = {
                    Text(
                            text = "Commit message",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                    )
                },
                colors = textFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.White,
                ),
        )
    }
}

@Composable
fun FileDiffItem(item: StatusViewItem.FileViewItem, model: StatusModel) {
    val fileStatus = item.fileStatus
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dp(8f))
                    .border(
                            width = Dp(1f),
                            color = Color.Gray,
                            shape = RoundedCornerShape(CornerSize(Dp(4f))),
                    ),
    ) {
        Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .clickable { model.notifyItemClicked(item) }
        ) {
            Text(
                    modifier = Modifier
                            .padding(Dp(8f)),
                    text = fileStatus.path,
                    color = fileStatus.status.toColor(),
            )
        }
        SelectionContainer {
            Text(
                    modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.DarkGray),
                    text = colorizeDiff(fileStatus.diff),
                    style = TextStyle(
                            color = Color.Gray,
                    ),

                    fontFamily = FontFamily.Monospace,
            )
        }
    }
}

private fun colorizeDiff(diff: String) = buildAnnotatedString {
    fun String.startsWithRune(rune: Char) = (this.length > 1 && this[0] == rune && this[1] != rune) || (this.length == 1 && this[0] == rune)

    diff.split("\n").forEach { line ->
        if (line.startsWith("diff --git")) {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.Gray)) {
                append(line)
                append("\n")
            }
            return@forEach
        }

        if (line.startsWithRune('-')) {
            withStyle(style = SpanStyle(color = Color.Red)) {
                append(line)
                append("\n")
            }
            return@forEach
        }

        if (line.startsWithRune('+')) {
            withStyle(style = SpanStyle(color = Color.Green)) {
                append(line)
                append("\n")
            }
            return@forEach
        }

        append(line)
        append("\n")
    }
}

private fun Status.toColor(): Color {
    return when (this) {
        Status.NEW -> Color.Green
        Status.MODIFIED -> Color.White
        Status.UNTRACKED -> Color.Red
    }
}

@Preview
@Composable
fun PreviewCommitScreen() {
    ComposeCommitScreen(StatusViewModel(
            listOf(
                    StatusViewItem.CommitViewItem("quick fix"),
                    StatusViewItem.FileViewItem(FileStatus(
                            path = "README.md",
                            status = Status.MODIFIED,
                            diff = "",
                    ))
            )
    ), TODO())
}
