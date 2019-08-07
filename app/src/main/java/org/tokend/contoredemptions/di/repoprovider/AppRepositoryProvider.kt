package org.tokend.contoredemptions.di.repoprovider

import androidx.collection.LruCache
import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.contoredemptions.base.data.repository.MemoryOnlyRepositoryCache
import org.tokend.contoredemptions.db.AppDatabase
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.contoredemptions.extensions.getOrPut
import org.tokend.contoredemptions.features.assets.data.storage.AssetsRepository
import org.tokend.contoredemptions.features.companies.data.CompaniesRepository
import org.tokend.contoredemptions.features.history.data.service.RedemptionsDbService
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsRepository
import org.tokend.contoredemptions.features.identity.storage.AccountDetailsRepository
import org.tokend.contoredemptions.features.transactions.storage.SystemInfoRepository

class AppRepositoryProvider(
        private val apiProvider: ApiProvider,
        private val database: AppDatabase,
        private val objectMapper: ObjectMapper,
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

    private val assetsRepository: AssetsRepository by lazy {
        AssetsRepository(apiProvider, objectMapper, urlConfigProvider, MemoryOnlyRepositoryCache())
    }

    private val accountDetailsRepository: AccountDetailsRepository by lazy {
        AccountDetailsRepository(apiProvider)
    }

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

    override fun assets(): AssetsRepository {
        return assetsRepository
    }

    override fun accountDetails(): AccountDetailsRepository {
        return accountDetailsRepository
    }

    private companion object {
        private const val MAX_SAME_REPOSITORIES_COUNT = 10
    }
}