package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.util.DebugLogger
import okhttp3.Interceptor
import java.net.InetSocketAddress
import java.net.Proxy


object TestDomainComponentFactory {
    data class ProxyConfig(
        val host: String,
        val port: Int,
    )
    fun create(
        responseInterceptor: Interceptor? = null,
        proxy: ProxyConfig? = null,
        ): DomainComponent<JdkPlatformDeps> {
        return DomainComponent(
            platformDeps = JdkPlatformDeps(),
            networkConfigurator = {
                val builder = this
                if (responseInterceptor != null) {
                    builder.addInterceptor(responseInterceptor)
                }

                if (proxy != null) {
                    builder.proxy(
                        Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port))
                    )
                }

                builder
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