package org.tokend.contoredemptions.features.redemption.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.features.assets.data.model.SimpleAsset
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.sdk.utils.extentions.isNotFound
import retrofit2.HttpException

class ValidateRedemptionRequestUseCase(
        private val request: RedemptionRequest,
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
                .redemptions()
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
                .onErrorResumeNext { error ->
                    if (error is HttpException && error.isNotFound())
                        Single.error(RedemptionAssetNotOwnException(SimpleAsset(request.assetCode)))
                    else
                        Single.error(error)
                }
                .flatMap { asset ->
                    repositoryProvider
                            .companies()
                            .updateIfNotFreshDeferred()
                            .andThen({
                                asset to repositoryProvider.companies().itemsMap
                            }.toSingle())
                }
                .flatMap { (asset, companiesMap) ->
                    if (!companiesMap.containsKey(asset.ownerAccountId))
                        Single.error(RedemptionAssetNotOwnException(asset))
                    else
                        Single.just(true)
                }
    }
}