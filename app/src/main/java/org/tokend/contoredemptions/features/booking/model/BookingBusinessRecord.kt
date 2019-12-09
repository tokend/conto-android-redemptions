package org.tokend.contoredemptions.features.booking.model

import org.tokend.sdk.api.integrations.booking.model.generated.resources.BusinessResource

class BookingBusinessRecord(
        val id: String,
        val calendarId: String,
        val rooms: List<BookingRoom>
) {
    companion object {
        fun fromResource(source: BusinessResource): BookingBusinessRecord {
            val specificDetails = source.bookingDetails.specificDetails
            val roomsMeta = source.details
                    ?.get("rooms_meta")
                    ?.fields()
                    ?.asSequence()
                    ?.map { it.key to it.value }
                    ?.toMap()
                    ?: throw IllegalArgumentException("Resource must have rooms meta in details")

            val rooms = specificDetails.map { (roomId, _) ->
                val meta = roomsMeta[roomId]

                BookingRoom(
                        id = roomId,
                        name = meta?.get("name")?.asText()
                                ?: throw IllegalStateException("No name for room $roomId"),
                        logoUrl = meta.get("logo_url")?.asText()
                )
            }

            return BookingBusinessRecord(
                    id = source.id,
                    calendarId = source.calendar.id,
                    rooms = rooms
            )
        }
    }
}