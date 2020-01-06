package org.tokend.contoredemptions.features.nfc.logic

import android.nfc.tech.IsoDep

class IsoDepNfcConnection(
        val isoDep: IsoDep
) : NfcConnection {
    override fun open() = isoDep.connect()

    override fun transceive(data: ByteArray): ByteArray = isoDep.transceive(data)

    override fun close() = isoDep.close()

    override val isActive: Boolean
        get() = isoDep.isConnected
}