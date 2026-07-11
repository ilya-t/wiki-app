package com.tsourcecode.wiki.lib.domain.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

class CoroutineScopes(
    val threading: Threading,
) {
    val worker = CoroutineScope(threading.io)
    val main = CoroutineScope(threading.main)

    fun close() {
        worker.cancel()
        main.cancel()
    }
}