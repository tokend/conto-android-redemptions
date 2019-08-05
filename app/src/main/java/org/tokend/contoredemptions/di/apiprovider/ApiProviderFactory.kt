package org.tokend.contoredemptions.di.apiprovider

import okhttp3.CookieJar
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.sdk.tfa.TfaCallback

class ApiProviderFactory {
    fun createApiProvider(urlConfigProvider: UrlConfigProvider,
                          tfaCallback: TfaCallback? = null,
                          cookieJar: CookieJar? = null,
                          withLogs: Boolean = true
    ): ApiProvider {
        return ApiProviderImpl(urlConfigProvider, tfaCallback, cookieJar, withLogs)
    }
}