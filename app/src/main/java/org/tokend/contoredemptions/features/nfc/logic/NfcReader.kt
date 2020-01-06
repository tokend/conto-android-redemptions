package org.tokend.contoredemptions.features.nfc.logic

import io.reactivex.Observable

interface NfcReader {
    fun start()

    fun stop()

    val isActive: Boolean

    val connections: Observable<NfcConnection>
}