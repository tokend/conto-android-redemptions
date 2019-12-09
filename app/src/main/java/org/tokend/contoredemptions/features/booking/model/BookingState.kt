package org.tokend.contoredemptions.features.booking.model

enum class BookingState(val value: Int) {
    PENDING(0),
    ACCEPTED(1),
    COMPLETED(2),
    CANCELED(3);

    companion object {
        fun fromValue(value: Int): BookingState {
            return when (value) {
                PENDING.value -> PENDING
                ACCEPTED.value -> ACCEPTED
                COMPLETED.value -> COMPLETED
                CANCELED.value -> CANCELED
                else -> throw IllegalArgumentException("There is no state with value '$value'")
            }
        }
    }
}