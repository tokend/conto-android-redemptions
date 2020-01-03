package org.tokend.contoredemptions.features.pos.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.features.assets.data.model.AssetRecord
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.pos.model.PosPaymentRequest
import org.tokend.contoredemptions.features.transactions.logic.TxManager
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.xdr.TransactionEnvelope
import java.math.BigDecimal

class AcceptPaymentWithPosTerminalUseCase(
        private val amount: BigDecimal,
        private val assetCode: String,
        private val posTerminal: PosTerminal,
        private val apiProvider: ApiProvider,
        private val repositoryProvider: RepositoryProvider,
        private val txManager: TxManager
) {
    private lateinit var networkParams: NetworkParams
    private lateinit var assetOwnerAccountId: String
    private lateinit var destinationBalanceId: String
    private lateinit var posPaymentRequest: PosPaymentRequest
    private lateinit var paymentTransaction: TransactionEnvelope
    private lateinit var transactionResultXdr: String

    fun perform(): Completable {
        return getNetworkParams()
                .doOnSuccess { networkParams ->
                    this.networkParams = networkParams
                }
                .flatMap {
                    getAssetOwnerAccountId()
                }
                .doOnSuccess { assetOwnerAccountId ->
                    this.assetOwnerAccountId = assetOwnerAccountId
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
                    getTransactionFromTerminal()
                }
                .doOnSuccess { paymentTransaction ->
                    this.paymentTransaction = paymentTransaction
                }
                .flatMap {
                    getSubmitTransactionResult()
                }
                .doOnSuccess {
                    updateRepositories()
                }
                .ignoreElement()
    }

    private fun getNetworkParams(): Single<NetworkParams> {
        return repositoryProvider.systemInfo()
                .getNetworkParams()
    }

    private fun getAssetOwnerAccountId(): Single<String> {
        return repositoryProvider.assets()
                .getSingle(assetCode)
                .map(AssetRecord::ownerAccountId)
    }

    private fun getDestinationBalanceId(): Single<String> {
        return apiProvider.getApi()
                .v3
                .accounts
                .getById(
                        accountId = assetOwnerAccountId,
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
                .switchIfEmpty(Single.error(IllegalStateException("Asset owner $assetOwnerAccountId " +
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

    private fun getSubmitTransactionResult(): Single<String> {
        return txManager.submit(paymentTransaction)
                .map { it.resultMetaXdr!! }
    }

    private fun updateRepositories() {
//        repositoryProvider
//                .redemptions(assetOwnerAccountId)
//                .add(RedemptionRecord(
//                        sourceAccount = RedemptionRecord.Account(
//                                id = tra
//                        )
//                ))
    }
}