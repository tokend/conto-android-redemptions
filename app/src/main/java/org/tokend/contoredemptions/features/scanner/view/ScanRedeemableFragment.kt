package org.tokend.contoredemptions.features.scanner.view

import android.util.LruCache
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toSingle
import org.spongycastle.util.encoders.DecoderException
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.features.booking.logic.BookingLoader
import org.tokend.contoredemptions.features.qr.view.ScanQrFragment
import org.tokend.contoredemptions.features.redemption.logic.DeserializeAndValidateRedemptionRequestUseCase
import org.tokend.contoredemptions.features.redemption.logic.RedemptionAlreadyProcessedException
import org.tokend.contoredemptions.features.redemption.logic.RedemptionAssetNotOwnException
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequestFormatException
import org.tokend.contoredemptions.features.scanner.model.RedeemableEntry
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.sdk.uri.TokenDUri
import org.tokend.sdk.utils.extentions.decodeBase64
import java.util.concurrent.TimeUnit

class ScanRedeemableFragment : ScanQrFragment<RedeemableEntry>() {
    private var scannedContent = ""
    private val invalidRequests = LruCache<String, Throwable>(10)
    private val bookingLoader: BookingLoader by lazy {
        BookingLoader(apiProvider)
    }

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

    override fun getResult(content: String): Single<out RedeemableEntry> {
        val visualTimeout = Completable.timer(500, TimeUnit.MILLISECONDS)

        val serializedBytes = try {
            content.decodeBase64()
        } catch (e: Exception) {
            return Single.error(e)
        }

        val getRedeemable: Single<out RedeemableEntry> =
                try {
                    val reference = TokenDUri.parse(String(serializedBytes, Charsets.UTF_8))
                            .takeIf { it.host == "booking.conto" }
                            ?.getQueryParam("reference")!!
                    getActiveBooking(reference)
                } catch (_: Exception) {
                    getRedemptionRequest(serializedBytes)
                }

        lateinit var redeemable: RedeemableEntry
        val getRedeemableCompletable = getRedeemable
                .doOnSuccess { redeemable = it }
                .ignoreElement()

        return Completable
                .mergeDelayError(listOf(visualTimeout, getRedeemableCompletable))
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .andThen({ redeemable }.toSingle())
    }

    private fun getRedemptionRequest(serializedRequest: ByteArray): Single<RedeemableEntry.RedemptionRequest> {
        return DeserializeAndValidateRedemptionRequestUseCase(
                serializedRequest,
                companyProvider.getCompany(),
                repositoryProvider
        )
                .perform()
                .map(RedeemableEntry::RedemptionRequest)
    }

    private fun getActiveBooking(reference: String): Single<RedeemableEntry.Booking> {
        return bookingLoader
                .load(reference)
                .map(RedeemableEntry::Booking)
    }

    override fun showQrScanErrorAndRetry(error: Throwable) {
        val message = when (error) {
            is RedemptionRequestFormatException,
            is DecoderException ->
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