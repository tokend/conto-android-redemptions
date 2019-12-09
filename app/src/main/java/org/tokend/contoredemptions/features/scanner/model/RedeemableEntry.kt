package org.tokend.contoredemptions.features.scanner.model

import org.tokend.contoredemptions.features.booking.model.BookingRecord

sealed class RedeemableEntry {
    class RedemptionRequest(val request: org.tokend.contoredemptions.features.redemption.model.RedemptionRequest)
        : RedeemableEntry()

    class Booking(val booking: BookingRecord)
        : RedeemableEntry()
}