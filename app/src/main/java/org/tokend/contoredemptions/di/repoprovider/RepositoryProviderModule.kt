package org.tokend.contoredemptions.di.repoprovider

import dagger.Module
import dagger.Provides
import org.tokend.contoredemptions.db.AppDatabase
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import javax.inject.Singleton

@Module
class RepositoryProviderModule {
    @Provides
    @Singleton
    fun repoProvider(
        apiProvider: ApiProvider,
        urlConfigProvider: UrlConfigProvider,
        database: AppDatabase
    ): RepositoryProvider {
        return AppRepositoryProvider(
            apiProvider,
            database,
            urlConfigProvider
        )
    }
}