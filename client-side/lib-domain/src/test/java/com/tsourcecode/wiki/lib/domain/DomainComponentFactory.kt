package com.tsourcecode.wiki.lib.domain

import okhttp3.Interceptor

object DomainComponentFactory {
    fun create(responseInterceptor: Interceptor): DomainComponent {
        return DomainComponent(
            platformDeps = JdkPlatformDeps(),
            networkConfigurator = { it.addInterceptor(responseInterceptor) },
        )
    }
}