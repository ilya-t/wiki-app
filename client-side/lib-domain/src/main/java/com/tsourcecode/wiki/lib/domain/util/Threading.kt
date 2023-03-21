package com.tsourcecode.wiki.lib.domain.util

import kotlinx.coroutines.CoroutineDispatcher

interface Threading {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
}
