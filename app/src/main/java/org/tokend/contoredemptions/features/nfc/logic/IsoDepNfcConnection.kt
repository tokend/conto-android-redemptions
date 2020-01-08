package org.tokend.contoredemptions.features.nfc.logic

import android.nfc.tech.IsoDep

class IsoDepNfcConnection(
        val isoDep: IsoDep
) : NfcConnection {
    override fun open() {
        isoDep.connect()
        isoDep.timeout = TRANSCEIVE_TIMEOUT_MS
    }

    override fun transceive(data: ByteArray): ByteArray = isoDep.transceive(data)

    override fun close() = isoDep.close()

    override val isActive: Boolean
        get() = isoDep.isConnected

    private companion object {
        private const val TRANSCEIVE_TIMEOUT_MS = 10000
    }
}