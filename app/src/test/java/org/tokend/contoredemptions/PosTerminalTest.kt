package org.tokend.contoredemptions

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Assert
import org.junit.Test
import org.tokend.contoredemptions.features.nfc.logic.NfcConnection
import org.tokend.contoredemptions.features.nfc.logic.NfcReader
import org.tokend.contoredemptions.features.pos.logic.PosTerminal
import org.tokend.contoredemptions.features.pos.model.ClientToPosResponse
import org.tokend.contoredemptions.features.pos.model.PosPaymentRequest
import org.tokend.contoredemptions.features.pos.model.PosToClientCommand
import org.tokend.sdk.utils.extentions.decodeBase64
import org.tokend.sdk.utils.extentions.encodeBase64String
import org.tokend.sdk.utils.extentions.encodeHexString
import org.tokend.wallet.*
import org.tokend.wallet.xdr.Fee
import org.tokend.wallet.xdr.Operation
import org.tokend.wallet.xdr.PaymentFeeData
import org.tokend.wallet.xdr.PaymentOp
import java.security.SecureRandom

class PosTerminalTest {
    val amount = 10L
    val balanceId = Base32Check.encodeBalanceId(SecureRandom.getSeed(32))
    val asset = "OLE"

    /**
     *  Receive payment request and send transaction in one connection.
     */
    @Test
    fun aHappyPath() {
        val dummyConnection = object : NfcConnection {
            var i = 0

            override fun open() {}

            override val isActive: Boolean
                get() = true

            override fun close() {}

            override fun transceive(data: ByteArray): ByteArray {
                i++
                println("Received ${data.encodeHexString()}")
                return when (val command = PosToClientCommand.fromBytes(data)) {
                    is PosToClientCommand.SelectAid -> {
                        if (i != 1) {
                            System.err.println("AID selection must be the first command")
                            Assert.fail()
                        }
                        ClientToPosResponse.Ok.data
                    }
                    is PosToClientCommand.SendPaymentRequest -> {
                        if (i != 2) {
                            System.err.println("Payment request must be the second command")
                            Assert.fail()
                        }
                        val paymentRequest = command.request
                        ClientToPosResponse.PaymentTransaction(
                                getTransactionForRequest(paymentRequest)
                        ).data
                    }
                    is PosToClientCommand.Error -> throw IllegalStateException("Error received")
                    else -> byteArrayOf()
                }.also {
                    println("Sent ${it.encodeHexString()}")
                }
            }
        }

        val dummyReader = object : NfcReader {
            override fun start() {}

            override fun stop() {}

            override val isActive: Boolean
                get() = true

            val connectionsSubject = PublishSubject.create<NfcConnection>()

            override val connections: Observable<NfcConnection>
                get() = connectionsSubject
        }

        val posTerminal = PosTerminal(dummyReader)
        val envelope = posTerminal.requestPayment(
                PosPaymentRequest(
                        precisedAmount = amount,
                        destinationBalanceId = balanceId,
                        assetCode = asset
                )
        )
                .doOnSubscribe { dummyReader.connectionsSubject.onNext(dummyConnection) }
                .blockingGet()
        println(envelope.toBase64())
    }

    /**
     * Receive payment request, interrupt the connection. Then send the transaction
     * by the second one.
     */
    @Test
    fun bTransactionAfterInterruption() {
        lateinit var reference: ByteArray
        lateinit var transaction: ByteArray

        val firstConnection = object : NfcConnection {
            override fun open() {}

            override val isActive: Boolean
                get() = true

            override fun close() {}

            override fun transceive(data: ByteArray): ByteArray {
                println("Received ${data.encodeHexString()}")
                return when (val command = PosToClientCommand.fromBytes(data)) {
                    is PosToClientCommand.SelectAid -> ClientToPosResponse.Ok.data
                    is PosToClientCommand.SendPaymentRequest -> {
                        val paymentRequest = command.request
                        reference = paymentRequest.reference
                        transaction = getTransactionForRequest(paymentRequest)
                        throw IllegalStateException("Oh no, phone was taken away!")
                    }
                    else -> throw IllegalStateException("We don't care")
                }.also {
                    println("Sent ${it.encodeHexString()}")
                }
            }
        }

        val secondConnection = object : NfcConnection {
            override fun open() {}

            override val isActive: Boolean
                get() = true

            override fun close() {}

            override fun transceive(data: ByteArray): ByteArray {
                println("Received ${data.encodeHexString()}")
                return when (val command = PosToClientCommand.fromBytes(data)) {
                    is PosToClientCommand.SelectAid -> ClientToPosResponse.Ok.data
                    is PosToClientCommand.SendPaymentRequest -> {
                        val paymentRequest = command.request
                        if (!paymentRequest.reference.contentEquals(reference)) {
                            System.err.println("Reference mismatch")
                            Assert.fail()
                        }
                        ClientToPosResponse.PaymentTransaction(transaction).data
                    }
                    else -> byteArrayOf()
                }.also {
                    println("Sent ${it.encodeHexString()}")
                }
            }
        }

        val dummyReader = object : NfcReader {
            override fun start() {}

            override fun stop() {}

            override val isActive: Boolean
                get() = true

            val connectionsSubject = PublishSubject.create<NfcConnection>()

            override val connections: Observable<NfcConnection>
                get() = connectionsSubject
        }

        val posTerminal = PosTerminal(dummyReader)
        val envelope = posTerminal.requestPayment(
                PosPaymentRequest(
                        precisedAmount = amount,
                        destinationBalanceId = balanceId,
                        assetCode = asset
                )
        )
                .doOnSubscribe {
                    println("Initiate first connection")
                    dummyReader.connectionsSubject.onNext(firstConnection)

                    Thread {
                        Thread.sleep(1000)
                        println("Initiate second connection")
                        dummyReader.connectionsSubject.onNext(secondConnection)
                    }.start()
                }
                .blockingGet()

        Assert.assertEquals(transaction.encodeBase64String(), envelope.toBase64())
        println(envelope.toBase64())
    }

    private fun getTransactionForRequest(request: PosPaymentRequest): ByteArray {
        val account = Account.random()
        val balanceId = Base32Check.encodeBalanceId(SecureRandom.getSeed(32))

        return TransactionBuilder(NetworkParams("Passphrase"), account.accountId)
                .addSigner(account)
                .addOperation(
                        Operation.OperationBody.Payment(
                                PaymentOp(
                                        destination = PaymentOp.PaymentOpDestination.Balance(
                                                PublicKeyFactory.fromBalanceId(request.destinationBalanceId)
                                        ),
                                        reference = request.reference.encodeHexString(),
                                        amount = request.precisedAmount,
                                        subject = "",
                                        sourceBalanceID = PublicKeyFactory.fromBalanceId(balanceId),
                                        feeData = PaymentFeeData(
                                                Fee(0, 0, Fee.FeeExt.EmptyVersion()),
                                                Fee(0, 0, Fee.FeeExt.EmptyVersion()),
                                                false,
                                                PaymentFeeData.PaymentFeeDataExt.EmptyVersion()
                                        ),
                                        ext = PaymentOp.PaymentOpExt.EmptyVersion()
                                )
                        )
                )
                .build()
                .getEnvelope()
                .toBase64()
                .decodeBase64()
    }
}