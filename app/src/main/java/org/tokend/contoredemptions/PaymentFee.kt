package org.tokend.contoredemptions

import org.tokend.SimpleFeeRecord
import java.io.Serializable
import java.math.BigDecimal

class PaymentFee(
    val senderFee: SimpleFeeRecord,
    val recipientFee: SimpleFeeRecord,
    var senderPaysForRecipient: Boolean = false
): Serializable {
    val totalSenderFee: BigDecimal
        get() = senderFee.total +
                if (senderPaysForRecipient) recipientFee.total else BigDecimal.ZERO
}