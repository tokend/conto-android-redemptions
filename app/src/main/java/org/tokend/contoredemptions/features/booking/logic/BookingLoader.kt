package org.tokend.contoredemptions.features.booking.logic

import io.reactivex.Single
import io.reactivex.rxkotlin.toMaybe
import org.tokend.contoredemptions.di.apiprovider.ApiProvider
import org.tokend.contoredemptions.features.booking.model.BookingBusinessRecord
import org.tokend.contoredemptions.features.booking.model.BookingRecord
import org.tokend.rx.extensions.toSingle

class BookingLoader(
        private val apiProvider: ApiProvider,
        private val businessId: String = DEFAULT_BUSINESS_ID
) {
    private var business: BookingBusinessRecord? = null

    fun load(reference: String): Single<BookingRecord> {
        return getBusiness()
                .flatMap { business ->
                    getBookingByReference(business, reference)
                }
    }

    private fun getBusiness(): Single<BookingBusinessRecord> {
        return business
                .toMaybe()
                .switchIfEmpty(apiProvider.getApi()
                        .integrations
                        .booking
                        .getBusiness(businessId)
                        .toSingle()
                        .map(BookingBusinessRecord.Companion::fromResource)
                        .doOnSuccess { business = it }
                )
    }

    private fun getBookingByReference(business: BookingBusinessRecord,
                                      reference: String): Single<BookingRecord> {
        return apiProvider.getApi()
                .integrations
                .booking
                .getBookingByReference(reference)
                .toSingle()
                .map { BookingRecord(it, business) }
    }

    companion object {
        private const val DEFAULT_BUSINESS_ID = "1"
    }
}