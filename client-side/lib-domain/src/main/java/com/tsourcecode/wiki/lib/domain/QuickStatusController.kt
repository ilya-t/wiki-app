package com.tsourcecode.wiki.lib.domain

class QuickStatusController {
    var listener: ((StatusInfo) -> Unit)? = null

    fun udpate(status: QuickStatus) {
        listener?.invoke(StatusInfo((status)))
    }

    fun error(status: QuickStatus, e: Exception) {
        listener?.invoke(StatusInfo(status, e))
    }
}

data class StatusInfo(
        val status: QuickStatus,
        val error: java.lang.Exception? = null
)