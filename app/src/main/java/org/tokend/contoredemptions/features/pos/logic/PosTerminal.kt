package org.tokend.contoredemptions.features.pos.logic

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.SingleSubject
import org.tokend.contoredemptions.features.nfc.logic.NfcConnection
import org.tokend.contoredemptions.features.nfc.logic.NfcReader
import org.tokend.contoredemptions.features.pos.model.ClientToPosResponse
import org.tokend.contoredemptions.features.pos.model.PosPaymentRequest
import org.tokend.contoredemptions.features.pos.model.PosToClientCommand
import org.tokend.sdk.utils.extentions.decodeHex
import org.tokend.wallet.Base32Check
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.PaymentOp
import org.tokend.wallet.xdr.PublicKey
import org.tokend.wallet.xdr.Transaction
import org.tokend.wallet.xdr.utils.XdrDataInputStream
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.util.concurrent.Executors

class PosTerminal(
        private val reader: NfcReader
) : Closeable {
    private val compositeDisposable = CompositeDisposable()
    private val executorService = Executors.newCachedThreadPool {
        Thread(it).apply { name = "PosCommunicationThread" }
    }

    private lateinit var transactionsSubject: SingleSubject<Transaction>
    private var currentPaymentRequest: PosPaymentRequest? = null

    init {
        subscribeToConnections()
    }

    fun requestPayment(posPaymentRequest: PosPaymentRequest): Single<Transaction> {
        currentPaymentRequest = posPaymentRequest
        transactionsSubject = SingleSubject.create()
        return transactionsSubject
    }

    private fun subscribeToConnections() {
        reader
                .connections
                .subscribe(this::onNewConnection)
                .addTo(compositeDisposable)
    }

    private fun onNewConnection(connection: NfcConnection) {
        executorService.submit { communicate(connection) }
    }

    private fun communicate(connection: NfcConnection) {
        val currentRequest = this.currentPaymentRequest
                ?: return

        try {
            connection.open()
            beginCommunication(connection, currentRequest)
        } catch (e: Exception) {
            sendCommand(connection, PosToClientCommand.Error)
        } finally {
            try {
                connection.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun beginCommunication(
            connection: NfcConnection,
            currentRequest: PosPaymentRequest
    ) {
        // Select AID.
        var response = sendCommand(connection, PosToClientCommand.SelectAid(AID))
        require(response is ClientToPosResponse.Ok)

        // Send payment request.
        response = sendCommand(connection, PosToClientCommand.SendPaymentRequest(currentRequest))
        if (response is ClientToPosResponse.PaymentTransaction) {
            onPaymentTransactionReceived(connection, response, currentRequest)
        } else {
            connection.close()
        }
    }

    private fun onPaymentTransactionReceived(
            connection: NfcConnection,
            paymentTransactionResponse: ClientToPosResponse.PaymentTransaction,
            relatedRequest: PosPaymentRequest
    ) {
        val transaction = Transaction.fromXdr(XdrDataInputStream(
                ByteArrayInputStream(paymentTransactionResponse.transactionXdr)))

        val paymentOp =
                transaction.operations
                        .find { it.body is Operation.OperationBody.Payment }
                        ?.let { it.body as Operation.OperationBody.Payment }
                        ?.paymentOp
                        ?: throw IllegalStateException("Received transaction has no payments")

        val reference = paymentOp.reference.decodeHex()
        require(reference.contentEquals(relatedRequest.reference))

        val amount = paymentOp.amount
        require(amount == relatedRequest.precisedAmount)

        val destinationBalanceId =
                (paymentOp.destination as? PaymentOp.PaymentOpDestination.Balance)
                        ?.balanceID
                        ?.let { it as? PublicKey.KeyTypeEd25519 }
                        ?.ed25519
                        ?.wrapped
                        ?: throw IllegalStateException("Invalid payment destination type")
        require(destinationBalanceId.contentEquals(
                Base32Check.decodeAccountId(relatedRequest.destinationBalanceId)))

        currentPaymentRequest
        transactionsSubject.onSuccess(transaction)

        sendCommand(connection, PosToClientCommand.Ok)
    }

    private fun sendCommand(
            connection: NfcConnection,
            command: PosToClientCommand
    ): ClientToPosResponse {
        val responseBytes = connection.transcieve(command.data)
        return ClientToPosResponse.fromBytes(responseBytes)
    }

    override fun close() {
        compositeDisposable.dispose()
        executorService.shutdownNow()
    }

    companion object {
        private val AID = byteArrayOf()
    }
}