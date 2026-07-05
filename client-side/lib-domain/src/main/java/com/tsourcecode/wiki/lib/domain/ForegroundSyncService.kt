package com.tsourcecode.wiki.lib.domain

interface ForegroundSyncService {
    fun start()
    fun stop(keepNotification: Boolean = false)
}
