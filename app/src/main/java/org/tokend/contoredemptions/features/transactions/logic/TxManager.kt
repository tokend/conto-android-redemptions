package org.tokend.contoredemptions.features.transactions.logic

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.rx.extensions.toSingle
import org.tokend.sdk.api.transactions.model.SubmitTransactionResponse
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.TransactionEnvelope

/**
 * Manages transactions sending
 */
class TxManager(private val apiProvider: ApiProvider) {

    fun submit(transaction: Transaction): Single<SubmitTransactionResponse> =
            submit(transaction.getEnvelope())

    fun submit(envelope: TransactionEnvelope): Single<SubmitTransactionResponse> {
        return apiProvider.getApi()
                .v3
                .transactions
                .submit(envelope, true)
                .toSingle()
    }

    companion object {
        /**
         * @return transaction with given [operations] for [sourceAccountId] signed by [signer]
         */
        fun createSignedTransaction(
                networkParams: NetworkParams,
                sourceAccountId: String,
                signer: Account,
                vararg operations: Operation.OperationBody
        ): Single<Transaction> {
            return Single.defer {
                val transaction =
                        TransactionBuilder(
                                networkParams,
                                PublicKeyFactory.fromAccountId(sourceAccountId)
                        )
                                .apply {
                                    operations.forEach {
                                        addOperation(it)
                                    }
                                }
                                .build()

                transaction.addSignature(signer)

                Single.just(transaction)
            }.subscribeOn(Schedulers.newThread())
        }
    }
}