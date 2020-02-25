package org.tokend.contoredemptions.features.urlconfig.logic

import org.tokend.contoredemptions.base.data.repository.ObjectPersistence
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.contoredemptions.features.urlconfig.model.UrlConfig

/**
 * Manages network configuration of the app.
 */
class UrlConfigManager(
        private val urlConfigProvider: UrlConfigProvider,
        private val urlConfigPersistence: ObjectPersistence<UrlConfig>
) {
    private var listener: (() -> Unit)? = null

    /**
     * Sets given config to the provider and saves it to the persistence
     */
    fun set(config: UrlConfig) {
        urlConfigProvider.setConfig(config)
        urlConfigPersistence.saveItem(config)

        listener?.invoke()
    }

    /**
     * @return [UrlConfig] selected by user, null if it is absent or selection is unsupported
     */
    fun get(): UrlConfig? {
        return if (urlConfigProvider.hasConfig())
            urlConfigProvider.getConfig()
        else
            null
    }

    fun onConfigUpdated(listener: () -> Unit) {
        this.listener = listener
    }
}