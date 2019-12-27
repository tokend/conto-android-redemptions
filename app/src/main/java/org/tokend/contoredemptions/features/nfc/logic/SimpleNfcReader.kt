package org.tokend.contoredemptions.features.nfc.logic

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SimpleNfcReader(
        private val activity: Activity
) : NfcReader {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val connectionsSubject = PublishSubject.create<NfcConnection>()
    private var mIsActive: Boolean = false

    override val isActive: Boolean
        get() = mIsActive

    override val connections: Observable<NfcConnection> = connectionsSubject

    override fun start() {
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

    override fun stop() {
        if (adapter == null
                || !adapter.isEnabled
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return
        }

        adapter.disableReaderMode(activity)
    }

    private fun onTagDiscovered(tag: Tag) {
        IsoDep.get(tag)
                ?.let(::IsoDepNfcConnection)
                ?.also(connectionsSubject::onNext)
    }
}