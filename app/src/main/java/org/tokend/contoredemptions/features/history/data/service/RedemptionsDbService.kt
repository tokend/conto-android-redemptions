package org.tokend.contoredemptions.features.history.data.service

import android.util.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.doAsync
import org.tokend.contoredemptions.features.history.data.model.RedemptionDbEntity
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsDao
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingOrder
import org.tokend.sdk.api.base.params.PagingParams

class RedemptionsDbService(
        private val dao: RedemptionsDao
) : RedemptionsService {
    override fun getPage(companyId: String?,
                         pagingParams: PagingParams): Single<DataPage<RedemptionRecord>> {
        return Single.defer {
            val limit = pagingParams.limit ?: DEFAULT_LIMIT
            val cursor = pagingParams.cursor?.toLongOrNull() ?: Long.MAX_VALUE

            if (pagingParams.order == PagingOrder.ASC) {
                return@defer Single.error<DataPage<RedemptionRecord>>(
                        NotImplementedError("${PagingOrder.ASC} order is not implemented")
                )
            }

            val items =
                    if (companyId != null)
                        dao.getCompanyPageDesc(
                                companyId,
                                limit,
                                cursor
                        )
                    else
                        dao.getPageDesc(
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
            cleanupIfNeeded()
            Completable.complete()
        }.subscribeOn(Schedulers.io())
    }

    override fun isReferenceKnown(reference: Long): Single<Boolean> {
        return Single.defer {
            val exists = dao.countByReference(reference) > 0
            Single.just(exists)
        }.subscribeOn(Schedulers.io())
    }

    private fun cleanupIfNeeded() {
        doAsync {
            val count = dao.count()
            if (count >= CLEANUP_THRESHOLD + CLEANUP_DELETE_COUNT) {
                val idOnThreshold = dao.getOldestId(CLEANUP_THRESHOLD - 1)
                Log.i(LOG_TAG, "Cleanup is needed, delete older than $idOnThreshold")
                dao.deleteOlderThan(idOnThreshold)
            }
        }
    }

    private companion object {
        private const val LOG_TAG = "RedmptDbSvc"
        private const val DEFAULT_LIMIT = 40
        private const val CLEANUP_THRESHOLD = 150
        private const val CLEANUP_DELETE_COUNT = 50
    }
}