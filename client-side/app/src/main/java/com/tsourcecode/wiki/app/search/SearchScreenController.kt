package com.tsourcecode.wiki.app.search

import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
import com.tsourcecode.wiki.lib.domain.search.DocumentSearchResult
import com.tsourcecode.wiki.lib.domain.search.SearchModel
import com.tsourcecode.wiki.lib.domain.search.SearchResultView
import com.tsourcecode.wiki.lib.domain.search.SearchViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.Closeable

class SearchScreenController(
        activityNavigator: ActivityNavigator,
        activity: AppCompatActivity,
        searchModel: SearchModel,
) {
    init {
        activity.findViewById<View>(R.id.btn_search).setOnClickListener {
            activityNavigator.open(Screen.SEARCH)
        }

        ScreenBootstrapper(
                Screen.SEARCH,
                activityNavigator,
                bootPoint = {
                    val composeView: ComposeView = it.findViewById(R.id.search_container)
                    val collectJob = activity.lifecycleScope.launch {
                        searchModel.data.collect { viewModel ->
                            composeView.setContent {
                                Render(viewModel, searchModel)
                            }
                        }
                    }
                    Closeable {
                        collectJob.cancel()
                    }
                }
        )
    }

    @Composable
    private fun Render(viewModel: SearchViewModel, searchModel: SearchModel) {
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
                    value = viewModel.searchRequest,
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

            LazyColumn {
                items(viewModel.results) { item: SearchResultView ->
                    when (item) {
                        is DocumentSearchResult -> DocumentResultItem(item, searchModel)
                    }.apply { /*exhaustive*/ }
                }
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
