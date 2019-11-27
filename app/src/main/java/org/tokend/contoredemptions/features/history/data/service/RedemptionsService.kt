package org.tokend.contoredemptions.features.history.data.service

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.sdk.api.base.model.DataPage
import org.tokend.sdk.api.base.params.PagingParams

interface RedemptionsService {

    fun getPage(companyId: String?,
                pagingParams: PagingParams): Single<DataPage<RedemptionRecord>>

    fun add(redemptionRecord: RedemptionRecord): Completable

    fun isReferenceKnown(reference: Long): Single<Boolean>
}