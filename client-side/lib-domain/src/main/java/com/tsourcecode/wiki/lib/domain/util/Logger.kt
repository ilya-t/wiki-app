package com.tsourcecode.wiki.lib.domain.util

open class Logger(
    val prefix: String = "",
    private val performer: (String) -> Unit,
) {
    fun fork(prefix: String): Logger {
        return Logger(this.prefix + prefix,
            performer)
    }

    inline fun log(message: () -> String) {
        log(prefix + message.invoke())
    }

    open fun log(message: String) {
        performer(message)
    }
}