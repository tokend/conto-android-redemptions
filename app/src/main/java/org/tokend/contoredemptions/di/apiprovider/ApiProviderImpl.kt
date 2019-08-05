package org.tokend.contoredemptions.di.apiprovider

import okhttp3.CookieJar
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.keyserver.KeyServer
import org.tokend.sdk.tfa.TfaCallback
import org.tokend.sdk.utils.CookieJarProvider

class ApiProviderImpl(
        private val urlConfigProvider: UrlConfigProvider,
        private val tfaCallback: TfaCallback?,
        cookieJar: CookieJar?,
        private val withLogs: Boolean
) : ApiProvider {
    private val url: String
        get() = urlConfigProvider.getConfig().api

    private var cookieJarProvider = cookieJar?.let {
        object : CookieJarProvider {
            override fun getCookieJar(): CookieJar {
                return it
            }
        }
    }

    private var apiByHash: Pair<Int, TokenDApi>? = null

    override fun getApi(): TokenDApi = synchronized(this) {
        val hash = url.hashCode()

        val api = apiByHash
                ?.takeIf { (currentHash, _) ->
                    currentHash == hash
                }
                ?.second
                ?: TokenDApi(
                        url,
                        null,
                        tfaCallback,
                        cookieJarProvider,
                        withLogs = withLogs
                )

        apiByHash = Pair(hash, api)

        return api
    }

    override fun getKeyServer(): KeyServer {
        return KeyServer(getApi().wallets)
    }
}