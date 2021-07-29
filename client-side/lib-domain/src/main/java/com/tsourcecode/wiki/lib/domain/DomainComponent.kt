package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.backend.BackendController

class DomainComponent(
        private val platformDeps: PlatformDeps,
) {
    val backendController = BackendController(platformDeps)
}