package org.tokend.contoredemptions.features.redemption.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.wallet.NetworkParams

class DeserializeAndValidateRedemptionRequestUseCase(
        private val serializedRequest: ByteArray,
        private val company: CompanyRecord,
        private val repositoryProvider: RepositoryProvider
) {
    private lateinit var networkParams: NetworkParams
    private lateinit var request: RedemptionRequest

    fun perform(): Single<RedemptionRequest> {
        return getNetworkParams()
                .doOnSuccess { networkParams ->
                    this.networkParams = networkParams
                }
                .flatMap {
                    getRedemptionRequest()
                }
                .doOnSuccess { request ->
                    this.request = request
                }
                .flatMap {
                    checkReference()
                }
                .flatMap {
                    checkAsset()
                }
                .map {
                    request
                }
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
                .systemInfo()
                .getNetworkParams()
    }

    private fun getRedemptionRequest(): Single<RedemptionRequest> {
        return try {
            RedemptionRequest.fromSerialized(networkParams, serializedRequest).toSingle()
        } catch (e: Exception) {
            Single.error<RedemptionRequest>(e)
        }
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