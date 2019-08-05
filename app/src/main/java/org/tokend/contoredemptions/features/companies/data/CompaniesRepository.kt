package org.tokend.contoredemptions.features.companies.data

import io.reactivex.Single
import org.tokend.contoredemptions.base.data.repository.RepositoryCache
import org.tokend.contoredemptions.base.data.repository.SimpleMultipleItemsRepository
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.base.PageQueryParams
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.sdk.utils.extentions.isBadRequest
import org.tokend.sdk.utils.extentions.isNotFound
import retrofit2.HttpException

class CompaniesRepository(
    private val apiProvider: ApiProvider,
    private val urlConfigProvider: UrlConfigProvider,
    itemsCache: RepositoryCache<CompanyRecord>
) : SimpleMultipleItemsRepository<CompanyRecord>(itemsCache) {

    override fun getItems(): Single<List<CompanyRecord>> {

        val loader = SimplePagedResourceLoader({ nextCursor ->
            apiProvider.getApi()
                .integrations
                .dns
                .getBusinesses(
                    PageQueryParams(
                        PagingParamsV2(page = nextCursor),
                        null
                    )
                )
        })

        return loader
            .loadAll()
            .toSingle()
            .map { companiesResources ->
                companiesResources.map {
                    CompanyRecord(it, urlConfigProvider.getConfig())
                }
            }
            .onErrorReturn { error ->
                if (error is HttpException && (error.isBadRequest() || error.isNotFound()))
                    emptyList()
                else
                    throw error
            }
    }
}