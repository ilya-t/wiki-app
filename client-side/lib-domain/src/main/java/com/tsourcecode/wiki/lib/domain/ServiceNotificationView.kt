package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServiceNotificationView(
    quickStatusController: QuickStatusController,
) {
    private val _state = MutableStateFlow(NotificationView(""))
    val state: Flow<NotificationView> = _state.asStateFlow()

    init {
        quickStatusController.addListener { statusInfo ->
            _state.value = statusInfo.toNotificationView()
        }
    }
}

private fun StatusInfo.toNotificationView(): NotificationView {
    val title = if (error != null) {
        "${status.name}: ${error.message ?: "null"}"
    } else if (comment.isBlank()) {
        status.name
    } else {
        "${status.name}: $comment"
    }
    return NotificationView(title)
}
