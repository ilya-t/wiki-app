package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityForegroundState {
    private val _isInForeground = MutableStateFlow(true)
    val isInForeground: Flow<Boolean> = _isInForeground.asStateFlow()

    val inForeground: Boolean
        get() = _isInForeground.value

    fun setInForeground(inForeground: Boolean) {
        _isInForeground.value = inForeground
    }
}
