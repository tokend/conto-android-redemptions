package org.tokend.contoredemptions.features.nfc.logic

import java.io.Closeable

interface NfcConnection : Closeable {
    fun open()

    fun transcieve(data: ByteArray): ByteArray

    val isActive: Boolean
}