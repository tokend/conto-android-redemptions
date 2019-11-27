package org.tokend.contoredemptions.features.redemption.view

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_confirm_redemption.*
import kotlinx.android.synthetic.main.appbar.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.layout_balance_change_main_data.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.onClick
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.redemption.logic.ConfirmRedemptionRequestUseCase
import org.tokend.contoredemptions.features.redemption.logic.RedemptionAlreadyProcessedException
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.features.transactions.logic.TxManager
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.util.formatter.AccountIdFormatter
import org.tokend.contoredemptions.view.balancechange.BalanceChangeMainDataView
import org.tokend.contoredemptions.view.details.DetailsItem
import org.tokend.contoredemptions.view.details.adapter.DetailsItemsAdapter
import org.tokend.contoredemptions.view.util.ElevationUtil
import org.tokend.contoredemptions.view.util.ProgressDialogFactory
import org.tokend.sdk.utils.extentions.decodeBase64

class ConfirmRedemptionActivity : BaseActivity() {
    private var emailLoadingFinished: Boolean = false
    private var senderEmail: String? = null

    private lateinit var request: RedemptionRequest
    private lateinit var asset: Asset

    private val adapter = DetailsItemsAdapter()
    private lateinit var mainDataView: BalanceChangeMainDataView

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

        val asset = intent.getSerializableExtra(EXTRA_ASSET) as? Asset
        if (asset == null) {
            errorHandler.handle(
                    IllegalArgumentException(
                            "No $EXTRA_ASSET specified"
                    )
            )
            finish()
            return
        }
        this.asset = asset

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
        loadAndDisplayRequestorEmail()
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
        mainDataView = BalanceChangeMainDataView(appbar, amountFormatter, dateFormatter)
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
            is RedemptionAlreadyProcessedException -> showAlreadyProcessedRequestDialog()
            else -> errorHandlerFactory.getDefault().handle(error)
        }
    }

    private fun showAlreadyProcessedRequestDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(R.string.error_redemption_request_no_more_valid)
            .setPositiveButton(R.string.ok, null)
            .setOnDismissListener {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            .show()
    }

    private fun displayDetails() {
        mainDataView.displayOperationName(getString(R.string.operation_redemption))
        mainDataView.displayAmount(request.amount, asset, true)

        displayRequestor()
    }

    private fun displayRequestor() {
        adapter.addOrUpdateItem(
                DetailsItem(
                        id = REQUESTOR_ITEM_ID,
                        text = if (!emailLoadingFinished)
                            getString(R.string.loading_data)
                        else
                            senderEmail
                                    ?: AccountIdFormatter().formatShort(request.sourceAccountId),
                        hint = getString(R.string.redemption_account),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_account),
                        singleLineText = true
                )
        )
    }

    private fun loadAndDisplayRequestorEmail() {
        repositoryProvider
                .accountDetails()
                .getEmailByAccountId(request.sourceAccountId)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnEvent { _, _ ->
                    emailLoadingFinished = true
                }
                .doOnSuccess { email ->
                    senderEmail = email
                }
                .subscribeBy(
                        onSuccess = {
                            displayRequestor()
                        },
                        onError = {
                            displayRequestor()
                        }
                )
                .addTo(compositeDisposable)
    }

    companion object {
        private const val EXTRA_REDEMPTION = "extra_redemption"
        private const val EXTRA_ASSET = "extra_asset"

        private const val REQUESTOR_ITEM_ID = 1L

        fun getBundle(redemptionRequest: String,
                      asset: Asset) = Bundle().apply {
            putString(EXTRA_REDEMPTION, redemptionRequest)
            putSerializable(EXTRA_ASSET, asset)
        }
    }
}