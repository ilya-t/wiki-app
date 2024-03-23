package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.util.DebugLogger
import okhttp3.Interceptor

object TestDomainComponentFactory {
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
            DebugLogger.impl = {
                println(it)
            }
            quickStatusController.listener = { info: StatusInfo ->
                info.error?.let {
                    throw AssertionError("status: ${info.status} message: ${info.comment}", it)
                }
            }
        }
    }
}