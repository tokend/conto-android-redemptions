package org.tokend.contoredemptions.di.apiprovider

import dagger.Module
import dagger.Provides
import okhttp3.CookieJar
import org.tokend.contoredemptions.BuildConfig
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import javax.inject.Singleton

@Module
class ApiProviderModule(
        private val cookieJar: CookieJar?
) {
    @Provides
    @Singleton
    fun apiProvider(urlConfigProvider: UrlConfigProvider): ApiProvider {
        return ApiProviderFactory()
                .createApiProvider(
                        urlConfigProvider,
                        tfaCallback = null,
                        cookieJar = cookieJar,
                        withLogs = BuildConfig.WITH_LOGS
                )
    }
}