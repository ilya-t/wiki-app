package com.tsourcecode.wiki.lib.domain

object NoOpNotificationService : NotificationService {
    override fun postNotification(text: String) = Unit
}
