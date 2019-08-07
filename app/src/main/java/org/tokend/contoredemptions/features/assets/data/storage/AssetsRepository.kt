package org.tokend.contoredemptions.features.assets.data.storage

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.contoredemptions.base.data.repository.RepositoryCache
import org.tokend.contoredemptions.base.data.repository.SimpleMultipleItemsRepository
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.urlconfigprovider.UrlConfigProvider
import org.tokend.contoredemptions.extensions.mapSuccessful
import org.tokend.contoredemptions.features.assets.data.model.AssetRecord
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.v3.assets.params.AssetsPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader

class AssetsRepository(
        private val apiProvider: ApiProvider,
        private val objectMapper: ObjectMapper,
        private val urlConfigProvider: UrlConfigProvider,
        itemsCache: RepositoryCache<AssetRecord>
) : SimpleMultipleItemsRepository<AssetRecord>(itemsCache) {
    override fun getItems(): Single<List<AssetRecord>> {

        val loader = SimplePagedResourceLoader(
                { nextCursor ->
                    apiProvider.getApi().v3.assets.get(
                            AssetsPageParams(
                                    pagingParams = PagingParamsV2(page = nextCursor)
                            )
                    )
                }
        )

        return loader
                .loadAll()
                .toSingle()
                .map { assetResources ->
                    assetResources.mapSuccessful {
                        AssetRecord.fromResource(it, urlConfigProvider.getConfig(), objectMapper)
                    }
                }
    }

    /**
     * @return single asset info
     */
    fun getSingle(code: String): Single<AssetRecord> {
        return itemsCache.items
                .find { it.code == code }
                .toMaybe()
                .switchIfEmpty(
                        apiProvider.getApi()
                                .v3
                                .assets
                                .getById(code)
                                .toSingle()
                                .map {
                                    AssetRecord.fromResource(it, urlConfigProvider.getConfig(),
                                            objectMapper)
                                }
                                .doOnSuccess {
                                    itemsCache.updateOrAdd(it)
                                    broadcast()
                                }
                )
    }
}