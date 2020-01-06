package org.tokend.contoredemptions.features.pos.model

enum class PaymentAcceptanceState {
    WAITING_FOR_PAYMENT,
    SUBMITTING_TX,
    ACCEPTED
}