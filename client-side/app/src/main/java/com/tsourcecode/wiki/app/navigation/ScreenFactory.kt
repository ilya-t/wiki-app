package com.tsourcecode.wiki.app.navigation

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tsourcecode.wiki.app.config.ConfigScreenView
import com.tsourcecode.wiki.lib.domain.DomainComponent

class ScreenFactory(
        private val activity: AppCompatActivity,
        private val domainComponent: DomainComponent,
) {
    fun configScreen(): ScreenView {
        return ConfigScreenView(
                activity,
                domainComponent.configScreenModel,
                activity.lifecycleScope,
        )
    }
}