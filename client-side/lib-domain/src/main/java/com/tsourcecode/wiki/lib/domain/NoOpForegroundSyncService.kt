package com.tsourcecode.wiki.lib.domain

object NoOpForegroundSyncService : ForegroundSyncService {
    override fun start() = Unit
    override fun stop() = Unit
}
