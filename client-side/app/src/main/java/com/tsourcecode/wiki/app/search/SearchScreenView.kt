package com.tsourcecode.wiki.app.search

import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import com.tsourcecode.wiki.app.navigation.ScreenView
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.project.ProjectComponentProvider
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import com.tsourcecode.wiki.lib.domain.search.DocumentSearchResult
import com.tsourcecode.wiki.lib.domain.search.SearchModel
import com.tsourcecode.wiki.lib.domain.search.SearchResultView
import com.tsourcecode.wiki.lib.domain.search.SearchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URI

class SearchScreenView(activity: AppCompatActivity,
                       private val scope: CoroutineScope,
                       projectsRepository: ProjectsRepository,
                       private val projectComponents: ProjectComponentProvider) : ScreenView {
    private val searchHeight = Dp(48f)
    private val composeView = ComposeView(activity)
    private var searchJob: Job? = null

    override val view: View = composeView

    override fun handle(uri: URI): Boolean {
        if (!AppNavigator.isSearch(uri)) {
            return false
        }

        val projectName = uri.path.split("/")[1]
        val component = projectComponents.get(projectName) ?: return false
        searchJob?.cancel()
        searchJob = scope.launch {
            component.searchModel.data.collect {
                composeView.setContent {
                    Render(viewModel = it, searchModel = component.searchModel)
                }
            }
        }

        return true
    }

    override fun close() {
        searchJob?.cancel()
        searchJob = null
    }

    @Composable
    private fun Render(viewModel: SearchViewModel, searchModel: SearchModel) {
        LazyColumn(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.padding(bottom = searchHeight + Dp(8f))
        ) {
            items(viewModel.results) { item: SearchResultView ->
                when (item) {
                    is DocumentSearchResult -> DocumentResultItem(item, searchModel)
                }.apply { /*exhaustive*/ }
            }
        }

        Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.height(searchHeight)
        ) {
            InputView(viewModel, searchModel)
        }
    }

    @Composable
    private fun InputView(viewModel: SearchViewModel, searchModel: SearchModel) {
        val focusRequester = FocusRequester()
        Box(
                Modifier.onKeyEvent {
                    if (it.nativeKeyEvent.keyCode != KeyEvent.KEYCODE_ENTER) {
                        return@onKeyEvent false
                    }

                    viewModel.results.firstOrNull()?.let { firstResult ->
                        if (firstResult is DocumentSearchResult) {
                            searchModel.notifyItemClicked(firstResult)
                        }
                    }
                    return@onKeyEvent true
                }
        ) {
            TextField(
                    modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                    width = Dp(1f),
                                    color = Color.Gray,
                                    shape = RoundedCornerShape(CornerSize(Dp(4f))),
                            )
                            .focusRequester(focusRequester)
                    ,
                    value = viewModel.searchRequest,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                            onSearch = {
                                searchModel.notifySearchKeyTriggered(viewModel)
                            }
                    ),
                    onValueChange = { searchModel.search(it) },
                    label = {
                        Text(
                                text = "Search",
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

        LaunchedEffect(Unit) {
            if (viewModel.requestFocusAtLaunch) {
                focusRequester.requestFocus()
            }
        }
    }

    @Composable
    private fun DocumentResultItem(item: DocumentSearchResult, searchModel: SearchModel) {
        Column(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dp(8f))
                        .border(
                                width = Dp(1f),
                                color = Color.Gray,
                                shape = RoundedCornerShape(CornerSize(Dp(4f))),
                        ),
        ) {
            Box(
                    modifier = Modifier
                            .fillMaxWidth()
                            .clickable { searchModel.notifyItemClicked(item) }
            ) {
                Text(
                        modifier = Modifier
                                .padding(Dp(8f)),
                        text = item.document.relativePath,
                        color = Color.White,
                )
            }
        }

    }
}

