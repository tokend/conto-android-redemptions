package org.tokend.contoredemptions.features.scanner.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.features.booking.model.BookingRecord
import org.tokend.contoredemptions.features.qr.model.NoCameraPermissionException
import org.tokend.contoredemptions.features.qr.view.ScanQrFragment
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.features.scanner.model.RedeemableEntry
import org.tokend.contoredemptions.util.Navigator
import org.tokend.contoredemptions.util.ObservableTransformers

class ProcessRedeeemableFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_fragment_container, container, false)
    }

    override fun onInitAllowed() {
        repositoryProvider.systemInfo().updateIfNotFresh()

        toScan()
    }

    private fun toScan(error: String? = null) {
        val fragment = ScanRedeemableFragment().apply {
            arguments = ScanQrFragment.getBundle(error)
        }

        fragment
                .result
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .subscribeBy(
                        onSuccess = this::confirmRedeemableAndStartScan,
                        onError = this::onRedemptionProcessingError
                )
                .addTo(compositeDisposable)

        displayFragment(fragment)
    }

    private fun toCameraPermissionError() {
        val fragment = NoCameraPermissionFragment()

        fragment
                .retryButtonClick
                .subscribeBy { this.toScan() }
                .addTo(compositeDisposable)

        displayFragment(fragment)
    }

    private fun confirmRedeemableAndStartScan(redeemable: RedeemableEntry) {
        when (redeemable) {
            is RedeemableEntry.RedemptionRequest -> confirmRedemptionRequest(redeemable.request)
            is RedeemableEntry.Booking -> confirmBooking(redeemable.booking)
        }
    }

    private fun confirmRedemptionRequest(request: RedemptionRequest) {
        val networkParams = repositoryProvider
                .systemInfo()
                .item
                ?.toNetworkParams()
        if (networkParams == null) {
            onRedemptionProcessingError(
                    IllegalStateException("System info must be available instantly at this moment")
            )
            return
        }

        Navigator.from(this).openAcceptRedemption(
                request.serialize(networkParams),
                CONFIRM_REDEMPTION_REQUEST
        )
    }

    private fun confirmBooking(bookingRecord: BookingRecord) {
        Navigator.from(this).openBookingDetails(
                bookingRecord,
                VIEW_BOOKING_DETAILS_REQUEST
        )
    }

    private fun onRedemptionProcessingError(error: Throwable) {
        when (error) {
            is NoCameraPermissionException ->
                toCameraPermissionError()
            else -> {
                toScan(errorHandlerFactory.getDefault().getErrorMessage(error))
            }
        }
        errorLogger.log(error)
    }

    private fun displayFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
                .disallowAddToBackStack()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONFIRM_REDEMPTION_REQUEST ||
                requestCode == VIEW_BOOKING_DETAILS_REQUEST) {
            toScan()
        }
    }

    private companion object {
        private val CONFIRM_REDEMPTION_REQUEST = "confirm_redemption".hashCode() and 0xfff
        private val VIEW_BOOKING_DETAILS_REQUEST = "view_booking".hashCode() and 0xfff
    }
}