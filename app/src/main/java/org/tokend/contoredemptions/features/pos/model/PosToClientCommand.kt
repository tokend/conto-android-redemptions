package org.tokend.contoredemptions.features.pos.model

sealed class PosToClientCommand {
    class SelectAid(aid: ByteArray) : PosToClientCommand() {
        override val data = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00) +
                aid.size.toByte() + aid
    }

    class SendPaymentRequest(request: PosPaymentRequest) : PosToClientCommand() {
        override val data = byteArrayOf(0x01) + request.serialize()
    }

    object Ok : PosToClientCommand() {
        override val data = byteArrayOf(0x20)
    }

    object Error : PosToClientCommand() {
        override val data = byteArrayOf(0x40)
    }

    abstract val data: ByteArray
}