package com.tsourcecode.wiki.lib.domain.util

object DebugLogger {
    val inMemoryLogs = mutableListOf<String>()
    var impl: (String) -> Unit = {}

    fun log(vararg args: Any?) {
        impl.invoke(args.joinToString(" "))
    }
}