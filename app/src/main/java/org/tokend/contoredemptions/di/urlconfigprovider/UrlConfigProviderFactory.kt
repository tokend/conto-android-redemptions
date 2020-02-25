package org.tokend.contoredemptions.di.urlconfigprovider

import org.tokend.contoredemptions.features.urlconfig.model.UrlConfig

class UrlConfigProviderFactory {
    fun createUrlConfigProvider(): UrlConfigProvider {
        return UrlConfigProviderImpl()
    }

    fun createUrlConfigProvider(config: UrlConfig): UrlConfigProvider {
        return createUrlConfigProvider().apply { setConfig(config) }
    }
}