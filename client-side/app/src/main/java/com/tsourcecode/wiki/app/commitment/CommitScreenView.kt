package com.tsourcecode.wiki.app.commitment

import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.app.navigation.ActivityNavigator
import com.tsourcecode.wiki.app.navigation.Screen
import com.tsourcecode.wiki.lib.domain.commitment.StatusModel
import com.tsourcecode.wiki.lib.domain.commitment.StatusViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val useCompose = false

class CommitScreenView(
        private val lifecycle: LifecycleOwner,
        private val navigator: ActivityNavigator,
        private val statusModel: StatusModel,
) {
    init {
        navigator.data.observe(lifecycle) {
            if (it.screen == Screen.COMMIT) {
                statusModel.updateStatus()
                setupScreen(it.layout)
            }
        }
    }

    private fun setupScreen(root: View) {
        val composeView = root.findViewById<ComposeView>(R.id.commit_container)
        val recyclerView = root.findViewById<RecyclerView>(R.id.commit_recycler)
        val commitAdapter = CommitAdapter(statusModel)
        recyclerView.adapter = commitAdapter
        val reverse = false
        recyclerView.layoutManager = LinearLayoutManager(root.context, LinearLayoutManager.VERTICAL, reverse)
        lifecycle.lifecycleScope.launch {
            statusModel.statusFlow.collect { viewModel ->
                if (useCompose) {
                    composeView.setContent {
                        ComposeCommitScreen(viewModel)
                    }
                } else {
                    commitAdapter.viewModel = viewModel
                }
            }
        }
    }
}

@Composable
private fun ComposeCommitScreen(viewModel: StatusViewModel) {
    Column {
        Text(text = "msg.author")
        Text(text = "msg.body")
    }
}

@Preview
@Composable
fun PreviewCommitScreen() {
    ComposeCommitScreen(StatusViewModel())
}
