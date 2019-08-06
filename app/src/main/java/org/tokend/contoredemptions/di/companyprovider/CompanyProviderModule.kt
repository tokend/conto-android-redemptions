package org.tokend.contoredemptions.di.companyprovider

import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class CompanyProviderModule {
    @Provides
    @Singleton
    fun companyProvider(): CompanyProvider {
        return CompanyProviderImpl()
    }
}