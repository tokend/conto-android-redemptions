package org.tokend.contoredemptions.features.redemption.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.features.qr.model.NoCameraPermissionException
import org.tokend.contoredemptions.features.qr.view.ScanQrFragment
import org.tokend.contoredemptions.features.redemption.logic.RedemptionAlreadyProcessedException
import org.tokend.contoredemptions.features.redemption.logic.RedemptionAssetNotOwnException
import org.tokend.contoredemptions.features.redemption.logic.ValidateRedemptionRequestUseCase
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.features.transactions.storage.SystemInfoRepository
import org.tokend.contoredemptions.util.Navigator
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.sdk.utils.extentions.encodeBase64String
import java.util.concurrent.TimeUnit

class ProcessRedemptionFragment : BaseFragment() {
    private val systemInfoRepository: SystemInfoRepository
        get() = repositoryProvider.systemInfo()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_fragment_container, container, false)
    }

    override fun onInitAllowed() {
        ensureSystemInfoAndStartScan()
    }

    private fun ensureSystemInfoAndStartScan() {
        if (systemInfoRepository.isFresh) {
            toScan()
        } else {
            systemInfoRepository
                    .updateDeferred()
                    .compose(ObservableTransformers.defaultSchedulersCompletable())
                    .retryWhen { errors ->
                        errors.flatMap {
                            Flowable.just(true)
                                    .delay(SYSTEM_INFO_LOADING_RETRY_INTERVAL_S, TimeUnit.SECONDS)
                        }
                    }
                    .doOnSubscribe {
                        toLoading()
                    }
                    .subscribeBy(
                            onComplete = { toScan() },
                            onError = { errorHandlerFactory.getDefault().handle(it) }
                    )
        }
    }

    private fun toLoading() {
        displayFragment(LoadingFragment())
    }

    private fun toScan(error: String? = null) {
        val fragment = ScanRedemptionFragment().apply {
            arguments = ScanQrFragment.getBundle(error)
        }

        fragment
                .result
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .subscribeBy(
                        onSuccess = this::validateAndConfirmRedemption,
                        onError = this::onRedemptionProcessingError
                )
                .addTo(compositeDisposable)

        displayFragment(fragment)
    }

    private fun validateAndConfirmRedemption(request: RedemptionRequest) {
        val performValidation =
                ValidateRedemptionRequestUseCase(
                        request,
                        companyProvider.getCompany(),
                        repositoryProvider
                )
                        .perform()

        val visualTimeout = Completable.timer(500, TimeUnit.MILLISECONDS)

        Completable.mergeDelayError(listOf(performValidation, visualTimeout))
                .compose(ObservableTransformers.defaultSchedulersCompletable())
                .doOnSubscribe {
                    toLoading()
                }
                .subscribeBy(
                        onComplete = { confirmRedemptionAndStartScan(request) },
                        onError = this::onRedemptionProcessingError
                )
    }

    private fun toCameraPermissionError() {
        val fragment = NoCameraPermissionFragment()

        fragment
                .retryButtonClick
                .subscribeBy { this.toScan() }
                .addTo(compositeDisposable)

        displayFragment(fragment)
    }

    private fun confirmRedemptionAndStartScan(request: RedemptionRequest) {
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

        val asset = repositoryProvider
                .assets()
                .itemsList
                .find { it.code == request.assetCode }
        if (asset == null) {
            onRedemptionProcessingError(
                    IllegalStateException("Asset ${request.assetCode} must available instantly at this moment")
            )
            return
        }

        Navigator.from(this).openAcceptRedemption(
                request.serialize(networkParams).encodeBase64String(),
                asset,
                CONFIRM_REDEMPTION_REQUEST
        )
    }

    private fun onRedemptionProcessingError(error: Throwable) {
        when (error) {
            is NoCameraPermissionException ->
                toCameraPermissionError()
            is RedemptionAlreadyProcessedException ->
                toScan(getString(R.string.error_redemption_request_no_more_valid))
            is RedemptionAssetNotOwnException ->
                toScan(getString(
                        R.string.template_error_redemption_not_own_asset,
                        error.asset.name ?: error.asset.code
                ))
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
        if (requestCode == CONFIRM_REDEMPTION_REQUEST) {
            toScan()
        }
    }

    private companion object {
        private const val SYSTEM_INFO_LOADING_RETRY_INTERVAL_S = 2L
        private val CONFIRM_REDEMPTION_REQUEST = "confirm_redemption".hashCode() and 0xfff
    }
}