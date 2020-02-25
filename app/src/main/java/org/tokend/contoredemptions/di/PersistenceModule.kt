package org.tokend.contoredemptions.di

import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import org.tokend.contoredemptions.base.data.repository.ObjectPersistence
import org.tokend.contoredemptions.base.data.repository.ObjectPersistenceOnPrefs
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.features.urlconfig.model.UrlConfig
import org.tokend.contoredemptions.features.urlconfig.storage.UrlConfigPersistence
import javax.inject.Singleton

@Module
class PersistenceModule(
    private val persistencePreferences: SharedPreferences,
    private val networkPreferences: SharedPreferences
) {
    @Provides
    @Singleton
    fun urlConfigPersistence(): ObjectPersistence<UrlConfig> {
        return UrlConfigPersistence(networkPreferences)
    }

    @Provides
    @Singleton
    fun lastCompanyPersistence(): ObjectPersistence<CompanyRecord> {
        return ObjectPersistenceOnPrefs(
            CompanyRecord::class.java,
            persistencePreferences,
            "last_company"
        )
    }
}