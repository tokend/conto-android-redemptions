package org.tokend.contoredemptions.features.redemption.logic

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.toMaybe
import io.reactivex.rxkotlin.toSingle
import io.reactivex.schedulers.Schedulers
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.di.repoprovider.RepositoryProvider
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.redemption.model.RedemptionRequest
import org.tokend.contoredemptions.features.transactions.logic.TxManager
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.general.model.SystemInfo
import org.tokend.sdk.api.transactions.model.SubmitTransactionResponse
import org.tokend.sdk.api.v3.accounts.params.AccountParamsV3
import org.tokend.wallet.NetworkParams
import org.tokend.wallet.Transaction

class ConfirmRedemptionRequestUseCase(
    private val request: RedemptionRequest,
    private val company: CompanyRecord,
    private val repositoryProvider: RepositoryProvider,
    private val apiProvider: ApiProvider,
    private val txManager: TxManager
) {

    private lateinit var systemInfo: SystemInfo
    private lateinit var networkParams: NetworkParams
    private lateinit var senderBalanceId: String
    private lateinit var transaction: Transaction
    private lateinit var submitTransactionResponse: SubmitTransactionResponse
    private lateinit var record: RedemptionRecord

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
            .flatMap {
                getRecord()
            }
            .doOnSuccess { record ->
                this.record = record
            }
            .flatMap {
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
            .getById(
                request.sourceAccountId, AccountParamsV3(
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
                )
            )
    }

    private fun getTransaction(): Single<Transaction> {
        val accountId = company.id

        return {
            request.toTransaction(senderBalanceId, accountId, networkParams)
        }.toSingle().subscribeOn(Schedulers.newThread())
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

    private fun getRecord(): Single<RedemptionRecord> {
        return Single.zip(
            repositoryProvider
                .assets()
                .getSingle(request.assetCode),
            repositoryProvider
                .accountDetails()
                .getEmailByAccountId(request.sourceAccountId)
                .onErrorReturnItem(""),
            BiFunction { asset: Asset, email: String ->
                asset to email.takeIf(String::isNotEmpty)
            }
        )
            .map { (asset, email) ->
                RedemptionRecord(
                    request,
                    asset,
                    company,
                    email
                )
            }
    }

    private fun updateRepositories(): Single<Boolean> {
        return repositoryProvider
            .redemptions(company.id)
            .add(record)
            .toSingleDefault(true)
    }
}