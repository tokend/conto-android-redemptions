package org.tokend.contoredemptions.features.nfc.logic

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.VibrationEffect
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.vibrator

class SimpleNfcReader(
        private val activity: Activity
) : NfcReader {
    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private val connectionsSubject = PublishSubject.create<NfcConnection>()
    private var mIsActive: Boolean = false

    override val isActive: Boolean
        get() = mIsActive

    override val connections: Observable<NfcConnection> = connectionsSubject

    private var acceptTags: Boolean = true

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
        if (acceptTags) {
            acceptTags = false
            Thread {
                Thread.sleep(TIMEOUT_MS)
                acceptTags = true
            }.start()

            vibrate()
            IsoDep.get(tag)
                    ?.let(::IsoDepNfcConnection)
                    ?.also(connectionsSubject::onNext)
        }
    }

    private fun vibrate() {
        val vibrator = activity.vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(VIBRATION_DURATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(VIBRATION_DURATION_MS)
            }
        }
    }

    companion object {
        private const val VIBRATION_DURATION_MS = 100L
        private const val TIMEOUT_MS = 1000L
    }
}