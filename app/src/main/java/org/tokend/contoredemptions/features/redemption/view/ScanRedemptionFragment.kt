package org.tokend.contoredemptions.features.redemption.view

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_scan_qr.*
import org.jetbrains.anko.onClick
import org.spongycastle.util.encoders.DecoderException
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequestFormatException
import org.tokend.contoredemptions.util.Navigator
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.util.PermissionManager
import org.tokend.contoredemptions.view.util.AnimationUtil
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.base.params.PagingParamsV2
import org.tokend.sdk.api.generated.resources.AssetResource
import org.tokend.sdk.api.v3.assets.params.AssetsPageParams
import org.tokend.sdk.utils.SimplePagedResourceLoader
import org.tokend.sdk.utils.extentions.decodeBase64
import java.util.concurrent.TimeUnit

class ScanRedemptionFragment : BaseFragment() {
    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)
    private var hasCameraPermission: Boolean = false
    private var qrScanIsRequired: Boolean = false

    //TODO: replace with assets repository
    private var assets: List<AssetResource>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan_qr, container, false)
    }

    override fun onInitAllowed() {
        initTitle()
        initQrScanner()
        initButtons()

        tryOpenQrScanner()

        loadAssets()
    }

    private fun tryOpenQrScanner() {
        cameraPermission.check(
            fragment = this,
            action = {
                hasCameraPermission = true
                resumeQrPreviewIfAllowed()
            },
            deniedAction = {
                hasCameraPermission = false
                permission_required_layout.visibility = View.VISIBLE
            }
        )
    }

    private fun initTitle() {
        qr_scanner_title_text_view.text = getString(R.string.redemption_scan_hint)
    }

    private fun initQrScanner() {
        qrScanIsRequired = true
        qr_scanner_view.initializeFromIntent(
            IntentIntegrator.forSupportFragment(this)
                .setBeepEnabled(false)
                .setDesiredBarcodeFormats(listOf(BarcodeFormat.QR_CODE.name))
                .createScanIntent()
        )
        qr_scanner_view.statusView.visibility = View.GONE
    }

    private fun initButtons() {
        var torchIsOn = false

        flash_switch_button.onClick {
            torchIsOn = !torchIsOn

            qr_scanner_view.barcodeView.setTorch(torchIsOn)

            if (torchIsOn) {
                flash_switch_button.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_flash_on)
                )
                flash_switch_button.contentDescription = getString(R.string.disable_flash_action)
            } else {
                flash_switch_button.setImageDrawable(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_flash_off)
                )
                flash_switch_button.contentDescription = getString(R.string.enable_flash_action)
            }
        }

        permission_request_button.onClick {
            tryOpenQrScanner()
        }
    }

    private fun resumeQrPreviewIfAllowed() {
        if (hasCameraPermission) {
            permission_required_layout.visibility = View.GONE
            qr_scanner_view.resume()
        }
    }

    private fun resumeQrScanIfRequired() {
        if (qrScanIsRequired) {
            qr_scanner_view.decodeSingle(object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult) {
                    onScannerResult(result.text)
                }

                override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
            })
        }
    }

    private fun onScannerResult(result: String) {
        qrScanIsRequired = false

        repositoryProvider
            .systemInfo()
            .getNetworkParams()
            .compose(ObservableTransformers.defaultSchedulersSingle())
            .map {
                RedemptionRequest.fromSerialized(it, result.decodeBase64())
            }
            .map { request ->
                val companyId = companyProvider.getCompany().id

                assets?.find {
                    it.owner.id == companyId && it.id == request.assetCode
                } ?: throw WrongAssetException()
            }
            .subscribeBy(
                onSuccess = {
                    qrScanIsRequired = true
                    Navigator.from(this).openAcceptRedemption(result)
                },
                onError = this::onRequestParsingError
            )
            .addTo(compositeDisposable)
    }

    private fun onRequestParsingError(error: Throwable) {
        val errorString = when (error) {
            is WrongAssetException ->
                getString(R.string.error_redemption_wrong_asset, companyProvider.getCompany().name)
            is RedemptionRequestFormatException,
            is DecoderException -> {
                error.cause?.printStackTrace()
                getString(R.string.error_invalid_redemption_request)
            }
            else -> error.localizedMessage
        }
        showQrScanErrorAndRetry(errorString)
        resumeQrScanIfRequired()
    }

    private var hideQrScanErrorDisposable: Disposable? = null
    private fun showQrScanErrorAndRetry(error: String) {
        hideQrScanErrorDisposable?.dispose()

        qr_scan_error_text_view.text = error
        if (qr_scan_error_text_view.visibility != View.VISIBLE) {
            AnimationUtil.fadeInView(qr_scan_error_text_view)
        }

        val scheduleErrorFadeOut = {
            hideQrScanErrorDisposable =
                Observable.timer(1, TimeUnit.SECONDS)
                    .compose(ObservableTransformers.defaultSchedulers())
                    .subscribeBy(
                        onComplete = {
                            AnimationUtil.fadeOutView(qr_scan_error_text_view)
                        }
                    )
                    .addTo(compositeDisposable)
        }

        Observable.timer(1, TimeUnit.SECONDS)
            .compose(ObservableTransformers.defaultSchedulers())
            .subscribeBy(
                onComplete = {
                    scheduleErrorFadeOut()
                    qrScanIsRequired = true
                    resumeQrScanIfRequired()
                }
            )
            .addTo(compositeDisposable)
    }

    //TODO: remove after assets repository add
    private fun loadAssets() {
        SimplePagedResourceLoader(
            { nextCursor ->
                apiProvider.getApi().v3.assets.get(
                    AssetsPageParams(
                        pagingParams = PagingParamsV2(page = nextCursor)
                    )
                )
            }
        )
            .loadAll()
            .toSingle()
            .compose(ObservableTransformers.defaultSchedulersSingle())
            .map {
                toastManager.short("assets loaded")
                assets = it
            }
            .ignoreElement()
            .subscribe()
            .addTo(compositeDisposable)
    }

    override fun onResume() {
        super.onResume()
        resumeQrPreviewIfAllowed()
        resumeQrScanIfRequired()
    }

    override fun onPause() {
        super.onPause()
        qr_scanner_view.pause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermission.handlePermissionResult(requestCode, permissions, grantResults)
    }

    class WrongAssetException : Exception()
}