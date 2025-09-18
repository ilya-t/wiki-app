package com.tsourcecode.wiki.lib.domain.util

open class Logger(
    val prefix: String = "",
    val performer: (String) -> Unit,
) {
    fun fork(prefix: String): Logger {
        return Logger(this.prefix + prefix,
            performer)
    }

    inline fun log(message: () -> String) {
        performer(prefix + message.invoke())
    }
}