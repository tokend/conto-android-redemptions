package org.tokend.contoredemptions.di.urlconfigprovider

import org.tokend.contoredemptions.util.UrlConfig

class UrlConfigProviderFactory {
    fun createUrlConfigProvider(): UrlConfigProvider {
        return UrlConfigProviderImpl()
    }

    fun createUrlConfigProvider(config: UrlConfig): UrlConfigProvider {
        return createUrlConfigProvider().apply { setConfig(config) }
    }
}