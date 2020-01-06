package org.tokend.contoredemptions.features.pos.logic

import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.SingleSubject
import org.tokend.contoredemptions.features.nfc.logic.NfcCommunicationThreadsHolder
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
import org.tokend.wallet.xdr.TransactionEnvelope
import org.tokend.wallet.xdr.utils.XdrDataInputStream
import java.io.ByteArrayInputStream
import java.io.Closeable

/**
 * Accepts payments from clients over NFC.
 *
 * @see requestPayment
 */
class PosTerminal(
        private val reader: NfcReader
) : Closeable {
    private val compositeDisposable = CompositeDisposable()
    private val communicationThreadsHolder = NfcCommunicationThreadsHolder()

    private lateinit var transactionsSubject: SingleSubject<TransactionEnvelope>
    private var currentPaymentRequest: PosPaymentRequest? = null

    init {
        subscribeToConnections()
    }

    /**
     * Requires connected clients to craft transaction satisfying provided [paymentRequest].
     * Only one payment is accepted.
     */
    fun requestPayment(paymentRequest: PosPaymentRequest): Single<TransactionEnvelope> {
        currentPaymentRequest = paymentRequest
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
        val currentRequest = this.currentPaymentRequest

        if (currentRequest == null || !transactionsSubject.hasObservers()) {
            return
        }

        communicationThreadsHolder.submit(connection) {
            communicate(connection, currentRequest)
        }
    }

    private fun communicate(connection: NfcConnection,
                            currentRequest: PosPaymentRequest) {
        try {
            connection.open()
            beginCommunication(connection, currentRequest)
        } catch (e: Exception) {
            e.printStackTrace()
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
        val envelope = TransactionEnvelope.fromXdr(XdrDataInputStream(
                ByteArrayInputStream(paymentTransactionResponse.transactionEnvelopeXdr)))

        val paymentOp = envelope
                .tx
                .operations
                .find { it.body is Operation.OperationBody.Payment }
                ?.let { it.body as Operation.OperationBody.Payment }
                ?.paymentOp
                ?: throw IllegalStateException("Received transaction has no payments")

        val reference = paymentOp.reference.decodeHex()
        if (!reference.contentEquals(relatedRequest.reference)) {
            throw IllegalStateException("Reference mismatch")
        }

        val amount = paymentOp.amount
        if (amount != relatedRequest.precisedAmount) {
            throw IllegalStateException("Amount mismatch")
        }

        val destinationBalanceId = (paymentOp.destination as? PaymentOp.PaymentOpDestination.Balance)
                ?.balanceID
                ?.let { it as? PublicKey.KeyTypeEd25519 }
                ?.ed25519
                ?.wrapped
                ?: throw IllegalStateException("Invalid payment destination type")
        if (!destinationBalanceId.contentEquals(
                        Base32Check.decodeBalanceId(relatedRequest.destinationBalanceId))) {
            throw IllegalStateException("Destination mismatch")
        }

        currentPaymentRequest = null
        transactionsSubject.onSuccess(envelope)

        sendCommand(connection, PosToClientCommand.Ok)
    }

    private fun sendCommand(
            connection: NfcConnection,
            command: PosToClientCommand
    ): ClientToPosResponse {
        val responseBytes = connection.transceive(command.data)
        return ClientToPosResponse.fromBytes(responseBytes)
    }

    override fun close() {
        compositeDisposable.dispose()
        communicationThreadsHolder.shutdown()
    }

    companion object {
        private val AID = "F0436F6E746F504F53".decodeHex()
    }
}