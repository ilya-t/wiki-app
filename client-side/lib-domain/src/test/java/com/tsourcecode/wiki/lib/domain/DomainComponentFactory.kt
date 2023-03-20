package com.tsourcecode.wiki.lib.domain

object DomainComponentFactory {
    fun create(): DomainComponent {
        return DomainComponent(
            platformDeps = JdkPlatformDeps()
        )
    }
}