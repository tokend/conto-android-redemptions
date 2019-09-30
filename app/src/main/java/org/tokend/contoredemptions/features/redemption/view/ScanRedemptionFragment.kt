package org.tokend.contoredemptions.features.redemption.view

import android.util.LruCache
import io.reactivex.Completable
import io.reactivex.Single
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.features.qr.view.ScanQrFragment
import org.tokend.contoredemptions.features.redemption.logic.RedemptionAlreadyProcessedException
import org.tokend.contoredemptions.features.redemption.logic.RedemptionAssetNotOwnException
import org.tokend.contoredemptions.features.redemption.logic.ValidateRedemptionRequestUseCase
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequestFormatException
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.sdk.utils.extentions.decodeBase64
import org.tokend.wallet.NetworkParams
import java.util.concurrent.TimeUnit

class ScanRedemptionFragment : ScanQrFragment<RedemptionRequest>() {
    private var scannedContent = ""
    private val invalidRequests = LruCache<String, Throwable>(10)

    override fun getTitle(): String? = getString(R.string.redemption_scan_title)

    override fun handleQrCodeContent(content: String) {
        scannedContent = content

        val knownError = invalidRequests[content]
        if (knownError != null) {
            showQrScanErrorAndRetry(knownError)
        } else {
            super.handleQrCodeContent(content)
        }
    }

    override fun getResult(content: String): Single<RedemptionRequest> {
        return getNetworkParams()
                .map { networkParams ->
                    RedemptionRequest.fromSerialized(networkParams, content.decodeBase64())
                }
                .flatMap { request ->
                    validateRequest(request)
                }
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider
                .systemInfo()
                .getNetworkParams()
    }

    private fun validateRequest(request: RedemptionRequest): Single<RedemptionRequest> {
        val performValidation =
                ValidateRedemptionRequestUseCase(
                        request,
                        companyProvider.getCompany(),
                        repositoryProvider
                )
                        .perform()

        val visualTimeout = Completable.timer(500, TimeUnit.MILLISECONDS)

        return Completable
                .mergeDelayError(listOf(performValidation, visualTimeout))
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .toSingleDefault(request)
    }

    override fun showQrScanErrorAndRetry(error: Throwable) {
        val message = when (error) {
            is RedemptionRequestFormatException ->
                getString(R.string.error_invalid_redemption_request)
            is RedemptionAlreadyProcessedException ->
                getString(R.string.error_redemption_request_no_more_valid)
            is RedemptionAssetNotOwnException -> getString(
                    R.string.template_error_redemption_not_own_asset,
                    error.asset.name ?: error.asset.code
            )
            else -> null
        }

        if (message != null) {
            invalidRequests.put(scannedContent, error)
            showQrScanErrorAndRetry(message)
        } else {
            super.showQrScanErrorAndRetry(error)
        }
    }
}