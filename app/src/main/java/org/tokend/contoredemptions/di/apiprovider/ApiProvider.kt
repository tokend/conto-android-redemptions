package org.tokend.contoredemptions.di.apiprovider

import org.tokend.sdk.api.TokenDApi
import org.tokend.sdk.keyserver.KeyServer

interface ApiProvider {
    fun getApi(): TokenDApi
    fun getKeyServer(): KeyServer
}