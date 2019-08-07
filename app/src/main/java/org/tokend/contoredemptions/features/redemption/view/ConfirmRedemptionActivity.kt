package org.tokend.contoredemptions.features.redemption.view

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_confirm_redemption.*
import kotlinx.android.synthetic.main.appbar.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_main_data.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.enabled
import org.jetbrains.anko.onClick
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.features.assets.data.model.AssetRecord
import org.tokend.contoredemptions.features.assets.data.model.SimpleAsset
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.features.redemption.logic.ConfirmRedemptionRequestUseCase
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.logic.TxManager
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.view.MainDataView
import org.tokend.contoredemptions.view.details.DetailsItem
import org.tokend.contoredemptions.view.details.adapter.DetailsItemsAdapter
import org.tokend.contoredemptions.view.util.ElevationUtil
import org.tokend.contoredemptions.view.util.ProgressDialogFactory
import org.tokend.sdk.utils.extentions.decodeBase64
import java.math.BigDecimal

class ConfirmRedemptionActivity : BaseActivity() {

    //TODO: get from assets repository
    private val asset: AssetRecord? = null

    private lateinit var request: RedemptionRequest

    private val adapter = DetailsItemsAdapter()
    private lateinit var mainDataView: MainDataView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_redemption)

        val errorHandler = errorHandlerFactory.getDefault()

        val requestString = intent.getStringExtra(EXTRA_REDEMPTION)
        if (requestString == null) {
            errorHandler.handle(
                IllegalArgumentException(
                    "No $EXTRA_REDEMPTION specified"
                )
            )
            finish()
            return
        }

        try {
            val networkParams = repositoryProvider
                .systemInfo()
                .item
                ?.toNetworkParams()
                ?: throw IllegalArgumentException("No loaded network params found")

            request = RedemptionRequest.fromSerialized(networkParams, requestString.decodeBase64())
        } catch (e: Exception) {
            errorHandler.handle(e)
            finish()
            return
        }

        initViews()
        displayDetails()
    }

    private fun initViews() {
        initToolbar()
        initDataView()
        initDetailsList()
        initConfirmationButton()
    }

    private fun initToolbar() {
        toolbar.background = ColorDrawable(Color.WHITE)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
        ElevationUtil.initScrollElevation(details_list, appbar_elevation_view)
    }

    private fun initDataView() {
        (top_info_text_view.layoutParams as? LinearLayout.LayoutParams)?.also {
            it.topMargin = 0
            top_info_text_view.layoutParams = it
        }
        mainDataView = MainDataView(appbar, amountFormatter)
    }

    private fun initDetailsList() {
        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter
    }

    private fun initConfirmationButton() {
        confirm_button.onClick {
            acceptRedemption()
        }
    }

    private fun acceptRedemption() {
        val dialog = ProgressDialogFactory.getDialog(this)

        ConfirmRedemptionRequestUseCase(
            request,
            companyProvider,
            repositoryProvider,
            apiProvider,
            TxManager(apiProvider)
        )
            .perform()
            .compose(ObservableTransformers.defaultSchedulersCompletable())
            .doOnSubscribe { dialog.show() }
            .doOnTerminate { dialog.dismiss() }
            .subscribeBy(
                onComplete = this::onRedemptionConfirmed,
                onError = this::onRedemptionConfirmationError
            )
            .addTo(compositeDisposable)
    }

    private fun onRedemptionConfirmed() {
        toastManager.short(R.string.successfully_accepted_redemption)
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun onRedemptionConfirmationError(error: Throwable) {
        when (error) {
            is ConfirmRedemptionRequestUseCase.RedemptionAlreadyProcessedException -> {
                toastManager.long(R.string.error_redemption_request_no_more_valid)
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            else -> errorHandlerFactory.getDefault().handle(error)
        }
    }

    private fun displayDetails() {
        val asset = this.asset ?: return
        mainDataView.displayOperationName(getString(R.string.operation_redemption))
        mainDataView.displayAmount(request.amount, asset, true)

        displayRecipient()
    }

    private fun displayRecipient() {
        adapter.addData(
            DetailsItem(
                text = companyProvider.getCompany().name,
                hint = getString(R.string.tx_recipient),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_briefcase)
            )
        )
    }

    companion object {
        private const val EXTRA_REDEMPTION = "extra_redemption"

        fun getBundle(redemptionRequest: String) = Bundle().apply {
            putString(EXTRA_REDEMPTION, redemptionRequest)
        }
    }
}