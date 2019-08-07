package org.tokend.contoredemptions.features.redemption.view

import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.features.qr.view.ScanQrFragment
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequestFormatException
import org.tokend.sdk.utils.extentions.decodeBase64

class ScanRedemptionFragment: ScanQrFragment<RedemptionRequest>() {
    override fun getTitle(): String? = getString(R.string.redemption_scan_title)

    override fun getResult(content: String): RedemptionRequest {
        val networkParams = repositoryProvider
            .systemInfo()
            .item
            ?.toNetworkParams()
            ?: throw IllegalStateException("System info must be available instantly at this moment")

        return try {
            RedemptionRequest.fromSerialized(networkParams, content.decodeBase64())
        } catch (e: Exception) {
            throw Exception(getString(R.string.error_invalid_redemption_request))
        }
    }
}