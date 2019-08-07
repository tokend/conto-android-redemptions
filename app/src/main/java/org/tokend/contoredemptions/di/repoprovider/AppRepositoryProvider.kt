package org.tokend.contoredemptions.di.repoprovider

import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.contoredemptions.SystemInfoRepository
import org.tokend.contoredemptions.base.data.repository.MemoryOnlyRepositoryCache
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.contoredemptions.features.companies.data.CompaniesRepository

class AppRepositoryProvider(
    private val apiProvider: ApiProvider,
    private val objectMapper: ObjectMapper,
    private val urlConfigProvider: UrlConfigProvider
) : RepositoryProvider {

    private val companiesRepository: CompaniesRepository by lazy {
        CompaniesRepository(apiProvider, urlConfigProvider, MemoryOnlyRepositoryCache())
    }

    private val systemInfoRepository: SystemInfoRepository by lazy {
        SystemInfoRepository(apiProvider)
    }

    override fun companies(): CompaniesRepository {
        return companiesRepository
    }

    override fun systemInfo(): SystemInfoRepository {
        return systemInfoRepository
    }
}