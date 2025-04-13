package com.tsourcecode.wiki.lib.domain.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class JdkThreading : Threading {
    override val main: CoroutineDispatcher = Executors
        .newSingleThreadExecutor()
        .asCoroutineDispatcher()
    override val io: CoroutineDispatcher = Executors
        .newCachedThreadPool()
        .asCoroutineDispatcher()
}