package org.tokend.contoredemptions.features.history.data.storage

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.contoredemptions.base.data.repository.RepositoryCache
import org.tokend.contoredemptions.base.data.repository.pagination.PagedDataRepository
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.history.data.service.RedemptionsService
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParams

class RedemptionsRepository(
        private val companyId: String,
        private val service: RedemptionsService,
        itemsCache: RepositoryCache<RedemptionRecord>
) : PagedDataRepository<RedemptionRecord>(itemsCache) {
    override fun getPage(nextCursor: String?): Single<DataPage<RedemptionRecord>> {
        return service.getPage(
                companyId,
                PagingParams(
                        order = PagingOrder.DESC,
                        limit = PAGE_LIMIT,
                        cursor = nextCursor
                )
        )
    }

    fun add(redemptionRecord: RedemptionRecord): Completable {
        return service
                .add(redemptionRecord)
                .doOnComplete {
                    itemsCache.add(redemptionRecord)
                    broadcast()
                }
    }

    fun isReferenceKnown(reference: Long): Single<Boolean> {
        return service
            .isReferenceKnown(reference)
    }

    private companion object {
        private const val PAGE_LIMIT = 40
    }
}