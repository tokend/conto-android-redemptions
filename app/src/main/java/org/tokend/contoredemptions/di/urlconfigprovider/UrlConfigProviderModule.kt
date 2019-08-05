package org.tokend.contoredemptions.di.urlconfigprovider

import dagger.Module
import dagger.Provides
import org.tokend.contoredemptions.util.UrlConfig
import javax.inject.Singleton

@Module
class UrlConfigProviderModule(
    private val defaultConfig: UrlConfig?
) {
    private val factory = UrlConfigProviderFactory()

    @Provides
    @Singleton
    fun urlConfigProvider(): UrlConfigProvider {
        return if (defaultConfig != null)
            factory.createUrlConfigProvider(defaultConfig)
        else
            factory.createUrlConfigProvider()
    }
}