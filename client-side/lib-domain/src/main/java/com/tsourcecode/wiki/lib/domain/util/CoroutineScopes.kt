package com.tsourcecode.wiki.lib.domain.util

import kotlinx.coroutines.CoroutineScope

class CoroutineScopes(
    private val threading: Threading,
) {
    val worker = CoroutineScope(threading.io)
    val main = CoroutineScope(threading.main)
}