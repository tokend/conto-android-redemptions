package org.tokend.contoredemptions.features.history.data.service

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.contoredemptions.features.history.data.model.RedemptionDbEntity
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsDao
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingParams

class RedemptionsDbService(
        private val dao: RedemptionsDao
) : RedemptionsService {
    override fun getPage(companyId: String,
                         pagingParams: PagingParams): Single<DataPage<RedemptionRecord>> {
        return Single.defer {
            val limit = pagingParams.limit ?: DEFAULT_LIMIT
            val cursor = pagingParams.cursor?.toLongOrNull() ?: Long.MAX_VALUE

            val items = dao.getPageDesc(
                    companyId,
                    limit,
                    cursor
            )

            Single.just(
                    DataPage(
                            nextCursor = items.lastOrNull()?.uid?.toString(),
                            items = items.map(RedemptionDbEntity::toRecord),
                            isLast = items.size < limit
                    )
            )
        }.subscribeOn(Schedulers.io())
    }

    override fun add(redemptionRecord: RedemptionRecord): Completable {
        return Completable.defer {
            dao.insert(RedemptionDbEntity.fromRecord(redemptionRecord))
            Completable.complete()
        }.subscribeOn(Schedulers.io())
    }

    private companion object {
        private const val DEFAULT_LIMIT = 40
    }
}