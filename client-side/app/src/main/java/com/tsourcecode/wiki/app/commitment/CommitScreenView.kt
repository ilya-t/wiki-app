package com.tsourcecode.wiki.app.commitment

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.navigation.ScreenView
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.commitment.FileStatus
import com.tsourcecode.wiki.lib.domain.commitment.Status
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewItem
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewModel
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URI

class CommitScreenView(
    private val activity: AppCompatActivity,
    private val scope: CoroutineScope,
    private val projectComponentResolver: ProjectComponentResolver,
) : ScreenView {
    private val composeView = ComposeView(activity)
    private var scopeJob: Job? = null
    override val view: View = composeView

    override fun handle(uri: URI): Boolean {
        if (!AppNavigator.isChanges(uri)) {
            return false
        }

        val component = projectComponentResolver.tryResolve(uri) ?: return false
        val statusModel = component.statusModel
        scopeJob?.cancel()
        scopeJob = scope.launch {
            statusModel.statusFlow.collect { viewModel ->
                composeView.setContent {
                    ComposeCommitScreen(viewModel)
                }
            }
        }
        statusModel.notifyCommitScreenOpened()
        return true
    }

    override fun close() {
        scopeJob?.cancel()
    }
}

@Composable
private fun ComposeCommitScreen(viewModel: StatusViewModel) {
    LazyColumn {
        items(viewModel.items) { item ->
            when (item) {
                is StatusViewItem.CommitViewItem -> CommitItem(item)
                is StatusViewItem.FileViewItem -> FileDiffItem(item)
                is StatusViewItem.RevisionViewItem -> RevisionItem(item)
            }.apply { /*exhaustive*/ }
        }
    }
}

@Composable
fun CommitItem(item: StatusViewItem.CommitViewItem) {
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
            onValueChange = { item.updateCommitText(it) },
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
        Text(
            text = item.itemsInfo,
            color = Color.White,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun RevisionItem(item: StatusViewItem.RevisionViewItem) {
    Column(
        modifier = Modifier
            .padding(Dp(8f))
    ) {
        Text(
            text = "Current revision:\n${item.message}",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun FileDiffItem(item: StatusViewItem.FileViewItem) {
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { item.onFileClick() }
        ) {
            Text(
                modifier = Modifier
                    .padding(Dp(8f))
                    .fillMaxWidth(.9f),
                text = fileStatus.path,
                color = fileStatus.status.toColor(),
            )
            Image(
                modifier = Modifier
                    .padding(Dp(8f))
                    .fillMaxWidth()
                    .clickable { item.onRollbackClick() }
                ,
                painter = rememberVectorPainter(
                    image = ImageVector.vectorResource(id = R.drawable.ic_rollback)
                ),
                contentDescription = ""
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
    fun String.startsWithRune(rune: Char) =
        (this.length > 1 && this[0] == rune && this[1] != rune) || (this.length == 1 && this[0] == rune)

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
    ComposeCommitScreen(
        StatusViewModel(
            listOf(
                StatusViewItem.RevisionViewItem("<revision message>"),
                StatusViewItem.CommitViewItem("quick fix", "(info)") {},
                StatusViewItem.FileViewItem(
                    FileStatus(
                        path = "README.md",
                        status = Status.MODIFIED,
                        diff = """
                            diff --git a/io.md b/io.md
                            index 7328fea..3eb293d 100644
                            --- a/io.md
                            +++ b/io.md
                            @@ -148,8 +148,8 @@ cd server-side
                                     - [ ] remove anything new
                                     - [ ] future: seek for new files
                                     - [ ]
                            -    - [ ] expand/collapse
                                 - [ ] revert
                            +    - [ ] expand/collapse
                                 - [ ] partial commit
                                 - [ ]
                             - [ ] evernotes
                        """.trimIndent(),
                    ),
                    onFileClick = {},
                    onRollbackClick = {},
                ),
            )
        )
    )
}
