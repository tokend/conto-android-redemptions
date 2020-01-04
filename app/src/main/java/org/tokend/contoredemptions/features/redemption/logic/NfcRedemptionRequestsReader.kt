package org.tokend.contoredemptions.features.redemption.logic

import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.tokend.contoredemptions.features.nfc.logic.NfcConnection
import org.tokend.contoredemptions.features.nfc.logic.NfcReader
import org.tokend.sdk.utils.extentions.decodeHex
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Reads redemption requests broadcasted over NFC if it's available
 *
 * @see readRequests
 */
class NfcRedemptionRequestsReader(
        private val nfcReader: NfcReader
) : Closeable {
    private val compositeDisposable = CompositeDisposable()
    private val readRequestsSubject: PublishSubject<ByteArray> = PublishSubject.create()
    val readRequests: Observable<ByteArray> = readRequestsSubject

    private val executorService = Executors.newCachedThreadPool()

    init {
        subscribeToConnections()
    }

    private fun subscribeToConnections() {
        nfcReader
                .connections
                .debounce(500, TimeUnit.MILLISECONDS)
                .subscribeBy(
                        onNext = this::onNewConnection,
                        onError = { it.printStackTrace() }
                )
                .addTo(compositeDisposable)
    }

    private fun onNewConnection(connection: NfcConnection) {
        executorService.submit {
            try {
                readRedemptionRequest(connection)
            } catch (_: Exception) {
            }
        }
    }

    private fun readRedemptionRequest(connection: NfcConnection) {
        val response: ByteArray? = try {
            connection.open()
            connection.transceive(SELECT_AID_HEADER + AID.size.toByte() + AID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection.close()
        }

        if (response == null
                || response.isEmpty()
                || response.size == 1
                || response.first() != 0x01.toByte()) {
            return
        }

        readRequestsSubject.onNext(response.sliceArray(1 until response.size))
    }

    override fun close() {
        compositeDisposable.dispose()
        executorService.shutdownNow()
    }

    companion object {
        private val SELECT_AID_HEADER = "00A40400".decodeHex()
        private val AID = "F0436F6E746F21".decodeHex()
    }
}