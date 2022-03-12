package com.tsourcecode.wiki.app.search

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tsourcecode.wiki.app.R
import com.tsourcecode.wiki.lib.domain.AppNavigator
import com.tsourcecode.wiki.lib.domain.project.ProjectsRepository
import java.net.URI

class SearchScreenController(
        appNavigator: AppNavigator,
        activity: AppCompatActivity,
        projectsRepository: ProjectsRepository,
) {
    init {
        activity.findViewById<View>(R.id.btn_search).setOnClickListener {
            appNavigator.open(URI("settings://search/${projectsRepository.data.value.first().name}"))
        }
    }
}
