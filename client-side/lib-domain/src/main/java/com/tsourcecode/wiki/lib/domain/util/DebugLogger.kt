package com.tsourcecode.wiki.lib.domain.util

object DebugLogger {
    var impl: (String) -> Unit = {}

    fun log(vararg args: Any?) {
        impl.invoke(args.joinToString(" "))
    }
}