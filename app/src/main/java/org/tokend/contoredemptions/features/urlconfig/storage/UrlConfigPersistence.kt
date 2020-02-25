package org.tokend.contoredemptions.features.urlconfig.storage

import android.content.SharedPreferences
import org.tokend.contoredemptions.base.data.repository.ObjectPersistenceOnPrefs
import org.tokend.contoredemptions.features.urlconfig.model.UrlConfig

class UrlConfigPersistence(
        preferences: SharedPreferences
) : ObjectPersistenceOnPrefs<UrlConfig>(UrlConfig::class.java, preferences, "url_config")