package org.tokend.contoredemptions.features.pos.logic

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.subjects.BehaviorSubject
import org.tokend.contoredemptions.di.companyprovider.CompanyProvider
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.pos.model.PaymentAcceptanceState
import org.tokend.contoredemptions.features.pos.model.PosPaymentRequest
import org.tokend.contoredemptions.features.transactions.logic.TxManager
import org.tokend.wallet.Base32Check
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.xdr.PublicKey
import org.tokend.wallet.xdr.TransactionEnvelope
import java.math.BigDecimal
import java.util.*

class AcceptPaymentWithPosTerminalUseCase(
        private val amount: BigDecimal,
        private val asset: Asset,
        private val posTerminal: PosTerminal,
        private val repositoryProvider: RepositoryProvider,
        private val companyProvider: CompanyProvider,
        private val txManager: TxManager
) {
    private val assetCode = asset.code

    private lateinit var networkParams: NetworkParams
    private lateinit var company: CompanyRecord
    private lateinit var destinationBalanceId: String
    private lateinit var posPaymentRequest: PosPaymentRequest
    private lateinit var paymentTransaction: TransactionEnvelope

    fun perform(): Observable<PaymentAcceptanceState> {
        val resultSubject = BehaviorSubject.createDefault(PaymentAcceptanceState.LOADING_DATA)

        val chain = getNetworkParams()
                .doOnSuccess { networkParams ->
                    this.networkParams = networkParams
                }
                .flatMap {
                    getAssetOwnerCompany()
                }
                .doOnSuccess { assetOwnerCompany ->
                    this.company = assetOwnerCompany
                }
                .flatMap {
                    getDestinationBalanceId()
                }
                .doOnSuccess { destinationBalanceId ->
                    this.destinationBalanceId = destinationBalanceId
                }
                .flatMap {
                    getPosPaymentRequest()
                }
                .doOnSuccess { posPaymentRequest ->
                    this.posPaymentRequest = posPaymentRequest
                }
                .flatMap {
                    resultSubject.onNext(PaymentAcceptanceState.WAITING_FOR_PAYMENT)
                    getTransactionFromTerminal()
                }
                .doOnSuccess { paymentTransaction ->
                    this.paymentTransaction = paymentTransaction
                }
                .flatMap {
                    resultSubject.onNext(PaymentAcceptanceState.SUBMITTING_TX)
                    txManager.submit(paymentTransaction)
                }
                .flatMap {
                    updateRepositories()
                }
                .doOnSuccess {
                    resultSubject.onComplete()
                }
                .map {
                    PaymentAcceptanceState.ACCEPTED
                }
                .toObservable()

        return Observable.defer {
            // Otherwise chain won't be disposed by desposing the
            // only subscriber of the subject.
            val disposable = chain.subscribeBy(
                    resultSubject::onNext,
                    resultSubject::onError,
                    resultSubject::onComplete
            )

            resultSubject.doOnDispose { disposable.dispose() }
        }
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider.systemInfo()
                .getNetworkParams()
    }

    private fun getAssetOwnerCompany(): Single<CompanyRecord> {
        return companyProvider.getCompany().toSingle()
    }

    private fun getDestinationBalanceId(): Single<String> {
        val balancesRepository = repositoryProvider.balances(company.id)

        return balancesRepository
                .updateIfNotFreshDeferred()
                .andThen(Maybe.defer {
                    balancesRepository
                            .itemsList
                            .find { it.asset.code == assetCode }
                            ?.id
                            .toMaybe()
                })
                .switchIfEmpty(Single.error(IllegalStateException("Asset owner ${company.id} " +
                        "somehow has no $assetCode balance")))
    }

    private fun getPosPaymentRequest(): Single<PosPaymentRequest> {
        return PosPaymentRequest(
                precisedAmount = networkParams.amountToPrecised(amount),
                assetCode = assetCode,
                destinationBalanceId = destinationBalanceId
        ).toSingle()
    }

    private fun getTransactionFromTerminal(): Single<TransactionEnvelope> {
        return posTerminal.requestPayment(posPaymentRequest)
    }

    private fun updateRepositories(): Single<Boolean> {
        val sourceAccountId = (paymentTransaction.tx.sourceAccount as? PublicKey.KeyTypeEd25519)
                ?.ed25519
                ?.wrapped
                ?.let(Base32Check::encodeAccountId)
                ?: return Single.just(false)

        return repositoryProvider
                .redemptions(company.id)
                .add(RedemptionRecord(
                        sourceAccount = RedemptionRecord.Account(
                                id = sourceAccountId,
                                nickname = DEFAULT_SOURCE_ACCOUNT_NICKNAME
                        ),
                        company = RedemptionRecord.Company(company),
                        date = Date(),
                        amount = amount,
                        asset = asset,
                        reference = posPaymentRequest.reference.contentHashCode().toLong()
                ))
                .toSingleDefault(true)
    }

    companion object {
        private const val DEFAULT_SOURCE_ACCOUNT_NICKNAME = "terminal@local.device"
    }
}