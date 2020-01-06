package org.tokend.contoredemptions.features.pos.view

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_accept_pos_payment.*
import kotlinx.android.synthetic.main.include_error_empty_view.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.balances.data.model.BalanceRecord
import org.tokend.contoredemptions.features.balances.data.storage.BalancesRepository
import org.tokend.contoredemptions.features.nfc.logic.NfcReader
import org.tokend.contoredemptions.features.nfc.logic.SimpleNfcReader
import org.tokend.contoredemptions.features.pos.logic.AcceptPaymentWithPosTerminalUseCase
import org.tokend.contoredemptions.features.pos.logic.PosTerminal
import org.tokend.contoredemptions.features.pos.model.PaymentAcceptanceState
import org.tokend.contoredemptions.features.transactions.logic.TxManager
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.view.util.LoadingIndicatorManager
import org.tokend.contoredemptions.view.util.ProgressDialogFactory
import java.math.BigDecimal

class AcceptPosPaymentFragment : BaseFragment() {
    private val balancesRepository: BalancesRepository
        get() = repositoryProvider.balances(companyProvider.getCompany().id)

    private val loadingIndicator = LoadingIndicatorManager(
            showLoading = {
                swipe_refresh.isRefreshing = true
                if (balancesRepository.isNeverUpdated) {
                    error_empty_view.showEmpty(R.string.loading_data)
                }
            },
            hideLoading = { swipe_refresh.isRefreshing = false }
    )

    private var asset: Asset? = null
        set(value) {
            field = value
            onAssetChanged()
        }

    private var canAccept: Boolean = false
        set(value) {
            field = value
            accept_button.isEnabled = value
            accept_button.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                            if (value)
                                R.color.colorAccent
                            else
                                R.color.mainActionButtonDisabled)
                    )
        }

    private val nfcReader: NfcReader by lazy {
        SimpleNfcReader(requireActivity())
    }
    private val posTerminal: PosTerminal by lazy {
        PosTerminal(nfcReader)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_accept_pos_payment, container, false)
    }

    override fun onInitAllowed() {
        initSwipeRefresh()
        initAssetSelection()
        initAmountInput()
        initButtons()

        subscribeToBalances()

        update()

        canAccept = false
    }

    private fun initSwipeRefresh() {
        swipe_refresh.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.colorAccent))
        swipe_refresh.setOnRefreshListener { update(force = true) }
    }

    private fun initAssetSelection() {
        asset_picker_spinner.onItemSelected {
            this.asset = it
        }
    }

    private fun initAmountInput() {
        payment_amount_view.amountWrapper.onAmountChanged { _, _ ->
            updateAcceptAvailability()
        }
        payment_amount_view.amountWrapper.setAmount(PRE_FILLED_AMOUNT)
    }

    private fun initButtons() {
        accept_button.setOnClickListener {
            tryToAcceptPayment()
        }
    }

    private fun subscribeToBalances() {
        balancesRepository.loadingSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { isLoading ->
                    loadingIndicator.setLoading(isLoading)
                }
                .addTo(compositeDisposable)

        balancesRepository.itemsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { onBalancesUpdated() }
                .addTo(compositeDisposable)

        balancesRepository.errorsSubject
                .compose(ObservableTransformers.defaultSchedulers())
                .subscribe { error ->
                    val errorHandler = errorHandlerFactory.getDefault()
                    if (balancesRepository.isNeverUpdated) {
                        error_empty_view.showError(error, errorHandler) {
                            update(force = true)
                        }
                    } else {
                        errorHandler.handle(error)
                    }
                }
                .addTo(compositeDisposable)
    }

    private fun onBalancesUpdated() {
        displayAssets()
    }

    private fun displayAssets() {
        val company = companyProvider.getCompany()

        val assets = balancesRepository.itemsList
                .map(BalanceRecord::asset)
                .filter { it.ownerAccountId == company.id }
                .sortedBy { it.name ?: it.code }

        if (assets.isEmpty() && !balancesRepository.isNeverUpdated) {
            error_empty_view.showEmpty(R.string.error_no_assets)
        } else if (assets.isNotEmpty()) {
            error_empty_view.hide()
        }

        asset_picker_spinner.setItems(assets)

        updateAcceptAvailability()
    }

    private fun onAssetChanged() {
        asset?.trailingDigits?.also { assetTrailingDigits ->
            payment_amount_view.amountWrapper.maxPlacesAfterComa = assetTrailingDigits
        }
    }

    private fun updateAcceptAvailability() {
        canAccept = payment_amount_view.amountWrapper.scaledAmount.signum() > 0
                && asset != null
    }

    private fun update(force: Boolean = false) {
        if (!force) {
            balancesRepository.updateIfNotFresh()
        } else {
            balancesRepository.update()
        }
    }

    private fun tryToAcceptPayment() {
        val asset = this.asset
                ?: return
        val amount = payment_amount_view.amountWrapper.scaledAmount

        if (canAccept) {
            acceptPayment(amount, asset)
        }
    }

    private fun acceptPayment(amount: BigDecimal, asset: Asset) {
        var disposable: Disposable? = null

        val progress = ProgressDialogFactory.getDialog(
                requireContext(),
                R.string.loading_data
        ) { disposable?.dispose() }
                .apply {
                    setTitle(R.string.waiting_for_pos_payment)
                }

        disposable = AcceptPaymentWithPosTerminalUseCase(
                amount = amount,
                asset = asset,
                repositoryProvider = repositoryProvider,
                posTerminal = posTerminal,
                companyProvider = companyProvider,
                txManager = TxManager(apiProvider)
        )
                .perform()
                .compose(ObservableTransformers.defaultSchedulers())
                .doOnSubscribe { progress.show() }
                .doOnTerminate { progress.dismiss() }
                .subscribeBy(
                        onNext = { state ->
                            progress.setMessage(getString(when (state) {
                                PaymentAcceptanceState.WAITING_FOR_PAYMENT -> R.string.pos_payment_phones_hint
                                PaymentAcceptanceState.SUBMITTING_TX -> R.string.processing_progress
                                else -> R.string.loading_data
                            }))
                        },
                        onComplete = this::onPosPaymentAccepted,
                        onError = this::onPosPaymentError
                )
                .addTo(compositeDisposable)
    }

    private fun onPosPaymentAccepted() {
        AlertDialog.Builder(requireContext())
                .setMessage(R.string.pos_payment_accepted_successfully)
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun onPosPaymentError(error: Throwable) {
        val message = errorHandlerFactory.getDefault().getErrorMessage(error)
                ?: return

        AlertDialog.Builder(requireContext())
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    override fun onPause() {
        super.onPause()
        nfcReader.stop()
    }

    override fun onResume() {
        super.onResume()
        nfcReader.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        posTerminal.close()
    }

    companion object {
        private val PRE_FILLED_AMOUNT = BigDecimal.ONE

        fun newInstance() = AcceptPosPaymentFragment()
    }
}