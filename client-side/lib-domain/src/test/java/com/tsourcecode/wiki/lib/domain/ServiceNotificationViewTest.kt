package com.tsourcecode.wiki.lib.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceNotificationViewTest {
    @Test
    fun usesCommentWhenPresent() = runTest {
        val quickStatusController = QuickStatusController()
        val view = ServiceNotificationView(quickStatusController)

        quickStatusController.udpate(QuickStatus.SYNC, "syncing with backend")

        assertEquals(
            NotificationView("SYNC: syncing with backend"),
            view.state.first(),
        )
    }

    @Test
    fun usesStatusNameWhenCommentBlank() = runTest {
        val quickStatusController = QuickStatusController()
        val view = ServiceNotificationView(quickStatusController)

        quickStatusController.udpate(QuickStatus.COMMIT)

        assertEquals(NotificationView("COMMIT"), view.state.first())
    }

    @Test
    fun includesErrorMessage() = runTest {
        val quickStatusController = QuickStatusController()
        val view = ServiceNotificationView(quickStatusController)

        quickStatusController.error(QuickStatus.SYNC, RuntimeException("network failure"))

        assertEquals(
            NotificationView("SYNC: network failure"),
            view.state.first(),
        )
    }
}
