package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.tests.OpenInTest

@OpenInTest
class QuickStatusController() {
    private val listeners = mutableListOf<(StatusInfo) -> Unit>()

    fun addListener(listener: (StatusInfo) -> Unit) {
        listeners.add(listener)
    }

    private fun notifyListeners(status: StatusInfo) {
        listeners.forEach { it(status) }
    }

    fun udpate(status: QuickStatus, comment: String = "") {
        notifyListeners(StatusInfo(status, error = null, comment = comment))
    }

    fun error(e: Throwable) {
        notifyListeners(StatusInfo(QuickStatus.ERROR, e))
    }

    fun error(status: QuickStatus, e: Throwable) {
        notifyListeners(StatusInfo(status, e))
    }
}

data class StatusInfo(
        val status: QuickStatus,
        val error: Throwable? = null,
        val comment: String = "",
)
