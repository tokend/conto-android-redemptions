package org.tokend.contoredemptions.di.companyprovider

import dagger.Module
import dagger.Provides
import org.tokend.contoredemptions.base.data.repository.ObjectPersistence
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import javax.inject.Singleton

@Module
class CompanyProviderModule {
    @Provides
    @Singleton
    fun companyProvider(lastCompanyPersistence: ObjectPersistence<CompanyRecord>): CompanyProvider {
        return CompanyProviderImpl(lastCompanyPersistence)
    }
}