package org.tokend.contoredemptions.features.booking.model

import org.tokend.sdk.api.integrations.booking.model.generated.resources.BookingResource
import java.io.Serializable

class BookingRecord(
        val id: String,
        val time: BookingTime,
        val room: BookingRoom,
        val seatsCount: Int,
        val reference: String,
        val ownerAccount: String,
        val state: BookingState
): Serializable {
    constructor(source: BookingResource,
                business: BookingBusinessRecord
    ) : this(
            id = source.id,
            time = BookingTime(source.startTime, source.endTime),
            room = business.rooms.find { it.id == source.payload }
                    ?: throw IllegalStateException("Room ${source.payload} is not in business ${business.id}"),
            seatsCount = source.participants,
            reference = source.reference,
            ownerAccount = source.owner.id,
            state = BookingState.fromValue(source.state.value)
    )
}