package com.tsourcecode.wiki.app

import com.tsourcecode.wiki.lib.domain.util.Threading
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class AndroidThreading : Threading {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
}
