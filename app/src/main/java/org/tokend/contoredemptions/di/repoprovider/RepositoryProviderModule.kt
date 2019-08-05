package org.tokend.contoredemptions.di.repoprovider

import dagger.Module
import dagger.Provides
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.sdk.factory.JsonApiToolsProvider
import javax.inject.Singleton

@Module
class RepositoryProviderModule {
    @Provides
    @Singleton
    fun repoProvider(apiProvider: ApiProvider): RepositoryProvider {
        return AppRepositoryProvider(
                apiProvider,
                JsonApiToolsProvider.getObjectMapper()
        )
    }
}