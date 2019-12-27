package org.tokend.contoredemptions.features.pos.model

sealed class ClientToPosResponse {
    object Ok : ClientToPosResponse() {
        private val HEADER = byteArrayOf(0x20)
        fun isIt(responseBytes: ByteArray) = responseBytes.contentEquals(HEADER)

        override val data = HEADER
    }

    object NoData : ClientToPosResponse() {
        private val HEADER = byteArrayOf(0x21)
        fun isIt(responseBytes: ByteArray) = responseBytes.contentEquals(HEADER)

        override val data = HEADER
    }

    class PaymentTransaction(val transactionXdr: ByteArray) : ClientToPosResponse() {
        override val data = HEADER + transactionXdr

        companion object {
            private val HEADER = byteArrayOf(0x22)

            fun isIt(responseBytes: ByteArray) =
                    responseBytes.size > HEADER.size
                            && responseBytes.sliceArray(HEADER.indices).contentEquals(
                            HEADER
                    )

            fun fromBytes(responseBytes: ByteArray): PaymentTransaction {
                return PaymentTransaction(
                        transactionXdr = responseBytes.sliceArray(HEADER.size until responseBytes.size)
                )
            }
        }
    }

    abstract val data: ByteArray

    companion object {
        fun fromBytes(responseBytes: ByteArray): ClientToPosResponse {
            return when {
                Ok.isIt(responseBytes) -> Ok
                NoData.isIt(responseBytes) -> NoData
                PaymentTransaction.isIt(responseBytes) -> PaymentTransaction.fromBytes(responseBytes)

                else -> throw IllegalArgumentException("Unknown response")
            }
        }
    }
}