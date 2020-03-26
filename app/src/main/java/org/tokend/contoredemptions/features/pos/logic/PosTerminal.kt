package org.tokend.contoredemptions.features.pos.logic

import android.nfc.TagLostException
import android.util.Log
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
import org.tokend.sdk.utils.extentions.encodeHexString
import org.tokend.wallet.Base32Check
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.PaymentOp
import org.tokend.wallet.xdr.PublicKey
import org.tokend.wallet.xdr.TransactionEnvelope
import org.tokend.wallet.xdr.utils.XdrDataInputStream
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.util.concurrent.Executors

/**
 * Accepts payments from clients over NFC.
 *
 * @see requestPayment
 */
class PosTerminal(
        private val reader: NfcReader
) : Closeable {
    private val compositeDisposable = CompositeDisposable()
    private val communicationExecutorService = Executors.newFixedThreadPool(4)

    private lateinit var transactionsSubject: SingleSubject<TransactionEnvelope>
    private val currentPaymentRequests: MutableCollection<PosPaymentRequest> = mutableSetOf()

    init {
        subscribeToConnections()
    }

    /**
     * Requires connected clients to craft transaction satisfying provided [paymentRequest].
     * Only one payment is accepted.
     */
    fun requestPayment(paymentRequest: PosPaymentRequest): Single<TransactionEnvelope> =
            requestPayment(setOf(paymentRequest))

    /**
     * Requires connected clients to craft transaction satisfying one of provider [paymentRequests].
     * Only one payment is accepted.
     */
    fun requestPayment(paymentRequests: Collection<PosPaymentRequest>): Single<TransactionEnvelope> {
        currentPaymentRequests.clear()
        currentPaymentRequests.addAll(paymentRequests)
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
        val currentRequests = this.currentPaymentRequests

        if (currentRequests.isEmpty() || !transactionsSubject.hasObservers()) {
            return
        }

        communicationExecutorService.submit {
            communicate(connection, currentRequests)
        }
    }

    private fun communicate(connection: NfcConnection,
                            currentRequests: Collection<PosPaymentRequest>) {
        try {
            connection.open()
            beginCommunication(connection, currentRequests)
        } catch (e: Exception) {
            if (e !is TagLostException) {
                e.printStackTrace()
                sendCommand(connection, PosToClientCommand.Error)
            } else {
                Log.d(LOG_TAG, "Connection was lost")
            }
        } finally {
            try {
                connection.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun beginCommunication(
            connection: NfcConnection,
            currentRequests: Collection<PosPaymentRequest>
    ) {
        // Select AID.
        var response = sendCommand(connection, PosToClientCommand.SelectAid(AID))
        require(response is ClientToPosResponse.Ok)

        // Send payment request.
        response =
                if (currentRequests.size == 1)
                    sendCommand(connection,
                            PosToClientCommand.SendPaymentRequest(currentRequests.first()))
                else
                    sendCommand(connection,
                            PosToClientCommand.SendMultiplePaymentRequests(currentRequests))

        if (response is ClientToPosResponse.PaymentTransaction) {
            onPaymentTransactionReceived(connection, response, currentRequests)
        } else {
            connection.close()
        }
    }

    private fun onPaymentTransactionReceived(
            connection: NfcConnection,
            paymentTransactionResponse: ClientToPosResponse.PaymentTransaction,
            sentRequests: Collection<PosPaymentRequest>
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
        val relatedRequest = sentRequests
                .find { it.reference.contentEquals(reference) }
                ?: throw IllegalStateException("Received transaction is not related to any of " +
                        "sent payment requests")

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

        currentPaymentRequests.clear()
        transactionsSubject.onSuccess(envelope)

        sendCommand(connection, PosToClientCommand.Ok)
    }

    private fun sendCommand(
            connection: NfcConnection,
            command: PosToClientCommand
    ): ClientToPosResponse {
        Log.d(LOG_TAG, "Send ${command.data.encodeHexString()}")
        val responseBytes = connection.transceive(command.data)
        Log.d(LOG_TAG, "Received ${responseBytes.encodeHexString()}")
        return ClientToPosResponse.fromBytes(responseBytes)
    }

    override fun close() {
        compositeDisposable.dispose()
        communicationExecutorService.shutdownNow()
    }

    companion object {
        private const val LOG_TAG = "PosTerminal"
        private val AID = "F0436F6E746F504F53".decodeHex()
    }
}