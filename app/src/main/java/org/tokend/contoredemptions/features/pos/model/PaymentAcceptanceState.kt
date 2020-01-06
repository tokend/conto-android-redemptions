package org.tokend.contoredemptions.features.pos.model

enum class PaymentAcceptanceState {
    LOADING_DATA,
    WAITING_FOR_PAYMENT,
    SUBMITTING_TX,
    ACCEPTED
}