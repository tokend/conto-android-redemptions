package org.tokend.contoredemptions.features.pos.model

import java.io.ByteArrayInputStream

sealed class PosToClientCommand {
    class SelectAid(aid: ByteArray) : PosToClientCommand() {
        override val data = HEADER + aid.size.toByte() + aid

        companion object {
            private val HEADER = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)

            fun isIt(commandBytes: ByteArray) = commandBytes.size > HEADER.size + 1
                    && commandBytes.sliceArray(HEADER.indices).contentEquals(HEADER)

            fun fromBytes(commandBytes: ByteArray) = SelectAid(
                    commandBytes.sliceArray(HEADER.size + 1 until commandBytes.size)
            )
        }
    }

    class SendPaymentRequest(val request: PosPaymentRequest) : PosToClientCommand() {
        override val data = HEADER + request.serialize()

        companion object {
            private val HEADER = byteArrayOf(0x01)

            fun isIt(commandBytes: ByteArray) = commandBytes.size > HEADER.size
                    && commandBytes.sliceArray(HEADER.indices).contentEquals(HEADER)

            fun fromBytes(commandBytes: ByteArray) = SendPaymentRequest(
                    PosPaymentRequest.fromSerialized(
                            commandBytes.sliceArray(HEADER.size until commandBytes.size))
            )
        }
    }

    class SendMultiplePaymentRequests(requests: Collection<PosPaymentRequest>) : PosToClientCommand() {
        override val data = HEADER + requests.size.toByte() +
                requests
                        .map { request ->
                            val serialized = request.serialize()
                            (byteArrayOf(serialized.size.toByte()) + serialized).toList()
                        }
                        .flatten()
                        .toByteArray()

        companion object {
            private val HEADER = byteArrayOf(0x02)

            fun isIt(commandBytes: ByteArray) = commandBytes.size > HEADER.size + 1
                    && commandBytes.sliceArray(HEADER.indices).contentEquals(HEADER)

            fun fromBytes(commandBytes: ByteArray): SendMultiplePaymentRequests {
                val requestsCount = commandBytes[HEADER.size].toInt()

                if (requestsCount < 1) {
                    throw IllegalArgumentException("Requests count must be 1 or greater")
                }

                val requests = ByteArrayInputStream(commandBytes, HEADER.size + 1,
                        commandBytes.size - HEADER.size - 1).use { stream ->
                    (0..requestsCount).map { i ->
                        val requestSize = stream.read()

                        if (requestSize < 1) {
                            throw IllegalStateException("Invalid request size $requestSize")
                        }

                        val serializedRequest = ByteArray(requestSize).also { stream.read(it) }

                        PosPaymentRequest.fromSerialized(serializedRequest)
                    }
                }

                return SendMultiplePaymentRequests(requests)
            }
        }
    }

    object Ok : PosToClientCommand() {
        private val HEADER = byteArrayOf(0x20)

        fun isIt(commandBytes: ByteArray) = commandBytes.contentEquals(HEADER)

        override val data = HEADER
    }

    object Error : PosToClientCommand() {
        private val HEADER = byteArrayOf(0x40)

        fun isIt(commandBytes: ByteArray) = commandBytes.contentEquals(HEADER)

        override val data = HEADER
    }

    abstract val data: ByteArray

    companion object {
        fun fromBytes(commandBytes: ByteArray): PosToClientCommand {
            return when {
                SelectAid.isIt(commandBytes) -> SelectAid.fromBytes(commandBytes)
                SendPaymentRequest.isIt(commandBytes) -> SendPaymentRequest.fromBytes(commandBytes)
                Ok.isIt(commandBytes) -> Ok
                Error.isIt(commandBytes) -> Error
                else -> throw IllegalArgumentException("Unknown command")
            }
        }
    }
}