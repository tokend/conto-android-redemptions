package org.tokend.contoredemptions.features.nfc.logic

import java.io.Closeable

interface NfcConnection : Closeable {
    fun open()

    fun transceive(data: ByteArray): ByteArray

    val isActive: Boolean
}