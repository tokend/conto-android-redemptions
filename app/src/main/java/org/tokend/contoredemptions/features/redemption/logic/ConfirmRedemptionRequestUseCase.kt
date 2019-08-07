package org.tokend.contoredemptions.features.redemption.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.schedulers.Schedulers
import org.tokend.SimpleFeeRecord
import org.tokend.contoredemptions.PaymentFee
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.companyprovider.CompanyProvider
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.logic.TxManager
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.general.model.SystemInfo
import org.tokend.sdk.api.transactions.model.SubmitTransactionResponse
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction
import org.tokend.wallet.TransactionBuilder
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.PaymentFeeData
import org.tokend.wallet.xdr.op_extensions.SimplePaymentOp
import java.lang.Exception
import java.lang.IllegalStateException
import java.math.BigDecimal

class ConfirmRedemptionRequestUseCase(
    private val request: RedemptionRequest,
    private val companyProvider: CompanyProvider,
    private val repositoryProvider: RepositoryProvider,
    private val apiProvider: ApiProvider,
    private val txManager: TxManager
) {
    class RedemptionAlreadyProcessedException : Exception()

    private lateinit var systemInfo: SystemInfo
    private lateinit var networkParams: NetworkParams
    private lateinit var senderBalanceId: String
    private lateinit var transaction: Transaction
    private lateinit var submitTransactionResponse: SubmitTransactionResponse

    fun perform(): Completable {
        return getSystemInfo()
            .doOnSuccess { systemInfo ->
                this.systemInfo = systemInfo
                this.networkParams = systemInfo.toNetworkParams()
            }
            .flatMap {
                getSenderBalanceId()
            }
            .doOnSuccess { senderBalanceId ->
                this.senderBalanceId = senderBalanceId
            }
            .flatMap {
                getTransaction()
            }
            .doOnSuccess { transaction ->
                this.transaction = transaction
            }
            .flatMap {
                submitTransaction()
            }
            .doOnSuccess { submitTransactionResponse ->
                this.submitTransactionResponse = submitTransactionResponse
            }
            .flatMap {
                ensureActualSubmit()
            }
            .doOnSuccess {
                updateRepositories()
            }
            .ignoreElement()
    }

    private fun getSystemInfo(): Single<SystemInfo> {
        val systemInfoRepository = repositoryProvider.systemInfo()

        return systemInfoRepository
            .updateDeferred()
            .andThen(Single.defer {
                Single.just(systemInfoRepository.item!!)
            })
    }

    private fun getSenderBalanceId(): Single<String> {
        val signedApi = apiProvider.getApi()

        return signedApi.v3.accounts
            .getById(request.sourceAccountId, AccountParamsV3(
                listOf(AccountParamsV3.Includes.BALANCES)
            )
            )
            .map { it.balances }
            .toSingle()
            .flatMapMaybe {
                it.find { balanceResource ->
                    balanceResource.asset.id == request.assetCode
                }?.id.toMaybe()
            }
            .switchIfEmpty(
                Single.error(
                    IllegalStateException("No balance ID found for ${request.assetCode}")
                ))
    }

    private fun getTransaction(): Single<Transaction> {
        val accountId = companyProvider.getCompany().id

        val zeroFee = SimpleFeeRecord(BigDecimal.ZERO, BigDecimal.ZERO)
        val fee = PaymentFee(zeroFee, zeroFee)

        return Single.defer {
            val operation = SimplePaymentOp(
                sourceBalanceId = senderBalanceId,
                destAccountId = accountId,
                amount = networkParams.amountToPrecised(request.amount),
                subject = "",
                reference = request.salt.toString(),
                feeData = PaymentFeeData(
                    sourceFee = fee.senderFee.toXdrFee(networkParams),
                    destinationFee = fee.recipientFee.toXdrFee(networkParams),
                    sourcePaysForDest = false,
                    ext = PaymentFeeData.PaymentFeeDataExt.EmptyVersion()
                )
            )

            val transaction = TransactionBuilder(networkParams, request.sourceAccountId)
                .addOperation(Operation.OperationBody.Payment(operation))
                .setSalt(request.salt)
                .setTimeBounds(request.timeBounds)
                .build()

            transaction.addSignature(request.signature)

            Single.just(transaction)
        }.subscribeOn(Schedulers.newThread())
    }

    private fun submitTransaction(): Single<SubmitTransactionResponse> {
        return txManager.submit(transaction)
    }

    private fun ensureActualSubmit(): Single<Boolean> {
        val latestBlockBeforeSubmit = systemInfo.ledgersState[SystemInfo.LEDGER_CORE]?.latest
            ?: return Single.error(IllegalStateException("Cannot obtain latest core block"))

        val transactionBlock = submitTransactionResponse.ledger ?: 0

        // The exactly same transaction is always accepted without any errors
        // but if it wasn't the first submit the block number will be lower than the latest one.
        return if (transactionBlock <= latestBlockBeforeSubmit) {
            Single.error(RedemptionAlreadyProcessedException())
        } else {
            Single.just(true)
        }
    }

    private fun updateRepositories() {

    }
}