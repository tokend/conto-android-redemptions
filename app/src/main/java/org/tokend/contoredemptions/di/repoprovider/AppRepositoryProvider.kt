package org.tokend.contoredemptions.di.repoprovider

import androidx.collection.LruCache
import org.tokend.contoredemptions.features.transactions.storage.SystemInfoRepository
import org.tokend.contoredemptions.base.data.repository.MemoryOnlyRepositoryCache
import org.tokend.contoredemptions.db.AppDatabase
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.contoredemptions.extensions.getOrPut
import org.tokend.contoredemptions.features.companies.data.CompaniesRepository
import org.tokend.contoredemptions.features.history.data.service.RedemptionsDbService
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsRepository

class AppRepositoryProvider(
        private val apiProvider: ApiProvider,
        private val database: AppDatabase,
        private val urlConfigProvider: UrlConfigProvider
) : RepositoryProvider {

    private val companiesRepository: CompaniesRepository by lazy {
        CompaniesRepository(apiProvider, urlConfigProvider, MemoryOnlyRepositoryCache())
    }

    private val systemInfoRepository: SystemInfoRepository by lazy {
        SystemInfoRepository(apiProvider)
    }

    private val redemptionsRepositories =
            LruCache<String, RedemptionsRepository>(MAX_SAME_REPOSITORIES_COUNT)

    override fun companies(): CompaniesRepository {
        return companiesRepository
    }

    override fun systemInfo(): SystemInfoRepository {
        return systemInfoRepository
    }

    override fun redemptions(companyId: String): RedemptionsRepository {
        val key = companyId
        return redemptionsRepositories.getOrPut(key) {
            RedemptionsRepository(
                    companyId,
                    RedemptionsDbService(database.redemptionsDao),
                    MemoryOnlyRepositoryCache()
            )
        }
    }

    private companion object {
        private const val MAX_SAME_REPOSITORIES_COUNT = 10
    }
}