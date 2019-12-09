package org.tokend.contoredemptions.features.dashboard.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.dip
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.features.redemption.logic.DeserializeAndValidateRedemptionRequestUseCase
import org.tokend.contoredemptions.features.redemption.logic.NfcRedemptionRequestsReader
import org.tokend.contoredemptions.util.Navigator
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.view.util.FragmentFactory
import org.tokend.contoredemptions.view.util.LogoUtil
import org.tokend.contoredemptions.view.util.ProgressDialogFactory
import java.util.concurrent.TimeUnit

class DashboardActivity : BaseActivity() {

    private val fragmentFactory = FragmentFactory()
    private lateinit var nfcRequestsReader: NfcRedemptionRequestsReader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initToolbar()
        initTabs()
        initNfcReader()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        title = ""

        toolbar.addView(
                ImageView(this).apply {
                    layoutParams = Toolbar.LayoutParams(
                            Toolbar.LayoutParams.WRAP_CONTENT,
                            dip(24)
                    ).apply {
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }

                    setImageDrawable(
                            ContextCompat.getDrawable(
                                    this@DashboardActivity,
                                    R.mipmap.product_logo)
                    )

                    scaleType = ImageView.ScaleType.FIT_START
                }
        )

        toolbar.addView(
                ImageView(this).apply {
                    val size = dip(32)

                    layoutParams = Toolbar.LayoutParams(size, size).apply {
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        setMargins(0, 0,
                                context.resources.getDimensionPixelSize(R.dimen.standard_margin), 0)
                    }

                    scaleType = ImageView.ScaleType.CENTER_INSIDE

                    val c = companyProvider.getCompany()
                    LogoUtil.setLogo(this, c.name, c.logoUrl, size)

                    TooltipCompat.setTooltipText(this, getString(R.string.select_company_title))

                    setOnClickListener {
                        Navigator.from(this@DashboardActivity)
                                .openCompanies(COMPANY_SELECTION_REQUEST)
                    }
                }
        )

        toolbar.navigationIcon = null
    }

    private fun initTabs() {
        bottom_tabs.setOnNavigationItemSelectedListener {
            displayFragment(it.itemId)
            true
        }

        bottom_tabs.selectedItemId = R.id.scan
    }

    // region NFC
    private fun initNfcReader() {
        nfcRequestsReader = NfcRedemptionRequestsReader(this)
        nfcRequestsReader
                .readRequests
                .debounce(500, TimeUnit.MILLISECONDS)
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribeBy(
                        onNext = this::onNfcRedemptionRequestRead,
                        onError = {}
                )
                .addTo(compositeDisposable)
    }

    private fun onNfcRedemptionRequestRead(request: ByteArray) {
        var disposable: Disposable? = null

        val progress = ProgressDialogFactory.getDialog(this, R.string.processing_progress) {
            disposable?.dispose()
        }

        disposable = DeserializeAndValidateRedemptionRequestUseCase(
                request,
                companyProvider.getCompany(),
                repositoryProvider
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnSubscribe { progress.show() }
                .doOnEvent { _, _ -> progress.dismiss() }
                .subscribeBy(
                        onSuccess = { Navigator.from(this).openAcceptRedemption(request) },
                        onError = { errorHandlerFactory.getDefault().handle(it) }
                )
    }
    // endregion

    private fun displayFragment(id: Int) {
        when (id) {
            R.id.scan -> displayFragment(fragmentFactory.getProcessRedeemableFragment())
            R.id.history -> displayFragment(fragmentFactory.getHistoryFragment())
            else -> Log.e("Dashboard", "Unknown screen ID")
        }
    }

    private fun displayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .disallowAddToBackStack()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                COMPANY_SELECTION_REQUEST -> finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcRequestsReader.startReadingIfAvailable()
    }

    override fun onPause() {
        super.onPause()
        nfcRequestsReader.stopReading()
    }

    companion object {
        private val COMPANY_SELECTION_REQUEST = "select_company".hashCode() and 0xffff
    }
}
