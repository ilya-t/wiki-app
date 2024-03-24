package com.tsourcecode.wiki.lib.domain

import com.tsourcecode.wiki.lib.domain.util.DebugLogger
import okhttp3.Interceptor
import java.io.File
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
        filesRoot: File? = null,
        ): DomainComponent<JdkPlatformDeps> {
        val platformDeps = if (filesRoot != null) {
            JdkPlatformDeps(filesRoot)
        } else {
            JdkPlatformDeps()
        }
        return DomainComponent(
            platformDeps = platformDeps,
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