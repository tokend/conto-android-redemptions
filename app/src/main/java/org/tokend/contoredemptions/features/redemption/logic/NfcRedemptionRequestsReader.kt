package org.tokend.contoredemptions.features.redemption.logic

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.tokend.sdk.utils.extentions.decodeHex
import java.util.concurrent.Executors

/**
 * Reads redemption requests broadcasted over NFC if it's available
 *
 * @see readRequests
 */
class NfcRedemptionRequestsReader(
        private val activity: Activity
) {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    private val readRequestsSubject: PublishSubject<ByteArray> = PublishSubject.create()
    val readRequests: Observable<ByteArray> = readRequestsSubject

    private val executorService = Executors.newCachedThreadPool()

    fun startReadingIfAvailable() {
        if (adapter == null
                || !adapter.isEnabled
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return
        }

        adapter.enableReaderMode(
                activity,
                this::onTagDiscovered,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null
        )
    }

    fun stopReading() {
        if (adapter == null
                || !adapter.isEnabled
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return
        }

        adapter.disableReaderMode(activity)
    }

    private fun onTagDiscovered(tag: Tag) {
        executorService.submit {
            try {
                val tagIsoDep = IsoDep.get(tag)!!
                readRequestThroughIsoDep(tagIsoDep)
            } catch (_: Exception) {
            }
        }
    }

    private fun readRequestThroughIsoDep(isoDep: IsoDep) {
        val response: ByteArray? = try {
            isoDep.connect()
            isoDep.transceive(SELECT_AID_HEADER + AID.size.toByte() + AID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            isoDep.close()
        }

        if (response == null
                || response.isEmpty()
                || response.size == 1
                || response.first() != 0x01.toByte()) {
            return
        }

        readRequestsSubject.onNext(response.sliceArray(1 until response.size))
    }

    companion object {
        private val SELECT_AID_HEADER = "00A40400".decodeHex()
        private val AID = "F0436F6E746F21".decodeHex()
    }
}