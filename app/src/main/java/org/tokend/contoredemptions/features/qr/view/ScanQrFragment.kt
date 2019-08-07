package org.tokend.contoredemptions.features.qr.view

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
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.SingleSubject
import kotlinx.android.synthetic.main.fragment_scan_qr.*
import org.jetbrains.anko.onClick
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.features.qr.model.NoCameraPermissionException
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.util.PermissionManager
import org.tokend.contoredemptions.view.util.AnimationUtil
import java.util.concurrent.TimeUnit

abstract class ScanQrFragment<ResultType> : BaseFragment() {
    private val cameraPermission = PermissionManager(Manifest.permission.CAMERA, 404)
    private var hasCameraPermission: Boolean = false
    private var qrScanIsRequired: Boolean = false

    private val preSetError: String?
        get() = arguments?.getString(PRE_SET_ERROR_EXTRA)

    private val resultSubject = SingleSubject.create<ResultType>()
    val result: Single<ResultType> = resultSubject

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scan_qr, container, false)
    }

    override fun onInitAllowed() {
        initTitle()
        initQrScanner()
        initButtons()
        showPreSetErrorIfNeeded()

        cameraPermission.check(
            fragment = this,
            action = {
                hasCameraPermission = true
                resumeQrPreviewIfAllowed()
            },
            deniedAction = {
                hasCameraPermission = false
                resultSubject.onError(NoCameraPermissionException())
            }
        )
    }

    private fun initTitle() {
        qr_scanner_title_text_view.text = getTitle()
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
    }

    private fun showPreSetErrorIfNeeded() {
        preSetError?.also { preSetError ->
            qrScanIsRequired = false
            showQrScanErrorAndRetry(preSetError)
        }
    }

    private fun resumeQrPreviewIfAllowed() {
        if (hasCameraPermission) {
            qr_scanner_view.resume()
        }
    }

    private fun resumeQrScanIfRequired() {
        if (qrScanIsRequired) {
            qr_scanner_view.decodeSingle(object : BarcodeCallback {
                override fun barcodeResult(result: BarcodeResult) {
                    handleQrCodeContent(result.text)
                }

                override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
            })
        }
    }

    private fun handleQrCodeContent(content: String) {
        qrScanIsRequired = false
        try {
            resultSubject.onSuccess(getResult(content))
        } catch (e: Exception) {
            showQrScanErrorAndRetry(e.localizedMessage)
        }
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

    /**
     * @return text displayed above the scanner viewfinder
     */
    abstract fun getTitle(): String?

    /**
     * @return result parsed from QR code content
     * or throws [Exception] with message that will be
     * displayed below the scanner viewfinder
     */
    abstract fun getResult(content: String): ResultType

    companion object {
        private const val PRE_SET_ERROR_EXTRA = "pre_set_error"

        fun getBundle(preSetError: String?) = Bundle().apply {
            putString(PRE_SET_ERROR_EXTRA, preSetError)
        }
    }
}