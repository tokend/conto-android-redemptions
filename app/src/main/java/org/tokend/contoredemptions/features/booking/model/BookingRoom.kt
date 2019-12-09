package org.tokend.contoredemptions.features.booking.model

import java.io.Serializable

class BookingRoom(
        val id: String,
        val name: String,
        val logoUrl: String?
): Serializable {
    override fun equals(other: Any?): Boolean {
        return other is BookingRoom && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}