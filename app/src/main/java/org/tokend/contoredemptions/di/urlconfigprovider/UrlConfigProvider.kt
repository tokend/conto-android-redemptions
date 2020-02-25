package org.tokend.contoredemptions.di.urlconfigprovider

import org.tokend.contoredemptions.features.urlconfig.model.UrlConfig

interface UrlConfigProvider {
    fun hasConfig(): Boolean
    fun getConfig(): UrlConfig
    fun setConfig(config: UrlConfig)
}