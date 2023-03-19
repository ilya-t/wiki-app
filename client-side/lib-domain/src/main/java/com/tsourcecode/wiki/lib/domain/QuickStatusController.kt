package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.tests.OpenInTest

@OpenInTest
class QuickStatusController {
    var listener: ((StatusInfo) -> Unit)? = null

    fun udpate(status: QuickStatus, comment: String = "") {
        listener?.invoke(StatusInfo(status, error = null, comment = comment))
    }

    fun error(e: Exception) {
        listener?.invoke(StatusInfo(QuickStatus.ERROR, e))
    }

    fun error(status: QuickStatus, e: Exception) {
        listener?.invoke(StatusInfo(status, e))
    }
}

data class StatusInfo(
        val status: QuickStatus,
        val error: java.lang.Exception? = null,
        val comment: String = "",
)