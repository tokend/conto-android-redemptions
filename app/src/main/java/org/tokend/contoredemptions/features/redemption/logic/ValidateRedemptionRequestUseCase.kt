package org.tokend.contoredemptions.features.redemption.logic

import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest

class ValidateRedemptionRequestUseCase(
        private val request: RedemptionRequest,
        private val company: CompanyRecord,
        private val repositoryProvider: RepositoryProvider
) {
    fun perform(): Completable {
        return checkReference()
                .flatMap {
                    checkAsset()
                }
                .ignoreElement()
    }

    private fun checkReference(): Single<Boolean> {
        return repositoryProvider
                .redemptions(company.id)
                .isReferenceKnown(request.salt)
                .flatMap { isKnown ->
                    if (isKnown)
                        Single.error(RedemptionAlreadyProcessedException())
                    else
                        Single.just(true)
                }
    }

    private fun checkAsset(): Single<Boolean> {
        return repositoryProvider
                .assets()
                .getSingle(request.assetCode)
                .flatMap { asset ->
                    if (asset.ownerAccountId != company.id)
                        Single.error(RedemptionAssetNotOwnException(asset))
                    else
                        Single.just(true)
                }
    }
}