package com.tsourcecode.wiki.lib.domain

import okhttp3.Interceptor

object DomainComponentFactory {
    fun create(responseInterceptor: Interceptor? = null): DomainComponent<JdkPlatformDeps> {
        return DomainComponent(
            platformDeps = JdkPlatformDeps(),
            networkConfigurator = {
                if (responseInterceptor != null) {
                    return@DomainComponent addInterceptor(responseInterceptor)
                }
                this
            },
        ).apply {
            quickStatusController.listener = { info: StatusInfo ->
                info.error?.let {
                    throw AssertionError("status: ${info.status} message: ${info.comment}", it)
                }
            }
        }
    }
}