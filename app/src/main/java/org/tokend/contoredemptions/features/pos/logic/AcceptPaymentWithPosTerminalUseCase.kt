package org.tokend.contoredemptions.features.pos.logic

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.subjects.PublishSubject
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.companyprovider.CompanyProvider
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.pos.model.PaymentAcceptanceState
import org.tokend.contoredemptions.features.pos.model.PosPaymentRequest
import org.tokend.contoredemptions.features.transactions.logic.TxManager
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3
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
        private val apiProvider: ApiProvider,
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
        val resultSubject = PublishSubject.create<PaymentAcceptanceState>()

        getNetworkParams()
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
                .doOnSuccess {
                    resultSubject.onComplete()
                    updateRepositories()
                }
                .map {
                    PaymentAcceptanceState.ACCEPTED
                }
                .toObservable()
                .subscribe(resultSubject)

        return resultSubject
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider.systemInfo()
                .getNetworkParams()
    }

    private fun getAssetOwnerCompany(): Single<CompanyRecord> {
        return companyProvider.getCompany().toSingle()
    }

    private fun getDestinationBalanceId(): Single<String> {
        return apiProvider.getApi()
                .v3
                .accounts
                .getById(
                        accountId = company.id,
                        params = AccountParamsV3(listOf(
                                AccountParamsV3.Includes.BALANCES,
                                AccountParamsV3.Includes.BALANCES_ASSET
                        ))
                )
                .toSingle()
                .flatMapMaybe { account ->
                    account
                            .balances
                            .find { balance ->
                                balance.asset.id == assetCode
                            }
                            ?.id
                            .toMaybe()
                }
                .switchIfEmpty(Single.error(IllegalStateException("Asset owner $company " +
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

    private fun updateRepositories() {
        val sourceAccountId = (paymentTransaction.tx.sourceAccount as? PublicKey.KeyTypeEd25519)
                ?.ed25519
                ?.wrapped
                ?.let(Base32Check::encodeAccountId)
                ?: return

        repositoryProvider
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
    }

    companion object {
        private const val DEFAULT_SOURCE_ACCOUNT_NICKNAME = "terminal@local.device"
    }
}