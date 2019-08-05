package org.tokend.contoredemptions.di.urlconfigprovider

import org.tokend.contoredemptions.util.UrlConfig

interface UrlConfigProvider {
    fun hasConfig(): Boolean
    fun getConfig(): UrlConfig
    fun setConfig(config: UrlConfig)
}