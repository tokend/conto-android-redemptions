package org.tokend.contoredemptions.features.pos.model

import org.tokend.wallet.Base32Check
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.SecureRandom

class PosPaymentRequest(
        val precisedAmount: Long,
        val assetCode: String,
        val destinationBalanceId: String,
        val reference: ByteArray = SecureRandom.getSeed(REFERENCE_SIZE)
) {
    init {
        require(reference.size == REFERENCE_SIZE)
    }

    fun serialize(): ByteArray {
        val byteStream = ByteArrayOutputStream()

        DataOutputStream(byteStream).use { stream ->
            stream.writeByte(VERSION_BYTE)
            stream.writeLong(precisedAmount)
            assetCode.toByteArray(STRING_CHARSET).also {
                stream.write(it.size)
                stream.write(it)
            }
            stream.write(Base32Check.decodeBalanceId(destinationBalanceId))
            stream.write(reference)
        }

        return byteStream.toByteArray()
    }

    companion object {
        private const val REFERENCE_SIZE = 32
        private val STRING_CHARSET = Charsets.UTF_8
        const val VERSION_BYTE = 1
    }
}