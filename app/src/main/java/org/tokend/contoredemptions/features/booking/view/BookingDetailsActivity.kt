package org.tokend.contoredemptions.features.booking.view

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_booking_details.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.features.booking.model.BookingRecord
import org.tokend.contoredemptions.features.booking.model.BookingState
import org.tokend.contoredemptions.util.ObservableTransformers
import org.tokend.contoredemptions.util.formatter.AccountIdFormatter
import org.tokend.contoredemptions.view.details.DetailsItem
import org.tokend.contoredemptions.view.details.adapter.DetailsItemsAdapter
import org.tokend.contoredemptions.view.util.ElevationUtil
import java.text.SimpleDateFormat
import java.util.*

class BookingDetailsActivity : BaseActivity() {
    private val adapter = DetailsItemsAdapter()

    private lateinit var booking: BookingRecord

    private var nicknameLoadingFinished = false
    private var ownerNickname: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_details)

        val booking = intent.getSerializableExtra(BOOKING_EXTRA) as? BookingRecord
        if (booking == null) {
            errorHandlerFactory.getDefault().handle(IllegalArgumentException(
                    "No $BOOKING_EXTRA specified"
            ))
            finish()
            return
        }
        this.booking = booking

        initToolbar()
        initList()

        displayDetails()

        loadAndDisplayOwnerNickname()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        setTitle(R.string.booking)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initList() {
        details_list.adapter = adapter
        details_list.layoutManager = LinearLayoutManager(this)
        ElevationUtil.initScrollElevation(details_list, appbar_elevation_view)
    }

    private fun displayDetails() {
        val stateWarning = when (booking.state) {
            BookingState.PENDING -> getString(R.string.booking_state_waiting_for_payment)
            BookingState.CANCELED -> getString(R.string.booking_state_canceled)
            BookingState.COMPLETED -> getString(R.string.booking_state_expired)
            else -> null
        }

        if (stateWarning != null) {
            adapter.addData(
                    DetailsItem(
                            text = stateWarning,
                            hint = getString(R.string.booking_state),
                            icon = ContextCompat.getDrawable(this, R.drawable.ic_warning),
                            textColor = ContextCompat.getColor(this, R.color.error)
                    )
            )
        }

        adapter.addData(
                DetailsItem(
                        text = booking.room.name,
                        hint = getString(R.string.booking_room),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_location)
                )
        )

        displayAccount()

        adapter.addData(
                DetailsItem(
                        text = resources.getQuantityString(
                                R.plurals.seats,
                                booking.seatsCount,
                                booking.seatsCount
                        ),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_seat_desk)
                )
        )

        val dateFormatter = SimpleDateFormat("dd MMM", Locale.getDefault())
        val timeFormatter = android.text.format.DateFormat.getTimeFormat(this)
        val time = booking.time

        adapter.addData(
                DetailsItem(
                        text = getString(R.string.time_from) + " "
                                + dateFormatter.format(time.from) + " "
                                + timeFormatter.format(time.from),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_calendar)
                ),
                DetailsItem(
                        text = getString(R.string.time_until) + " "
                                + dateFormatter.format(time.to) + " "
                                + timeFormatter.format(time.to)
                )
        )
    }

    private fun displayAccount() {
        val accountIdFormatter = AccountIdFormatter()

        val text =
                if (nicknameLoadingFinished)
                    ownerNickname
                            ?: accountIdFormatter.formatShort(booking.ownerAccount)
                else
                    getString(R.string.loading_data)

        adapter.addOrUpdateItem(
                DetailsItem(
                        text = text,
                        hint = getString(R.string.account),
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_account),
                        id = ACCOUNT_ITEM_ID
                )
        )
    }

    private fun loadAndDisplayOwnerNickname() {
        repositoryProvider
                .accountDetails()
                .getEmailByAccountId(booking.ownerAccount)
                .compose(ObservableTransformers.defaultSchedulersSingle())
                .doOnEvent { _, _ ->
                    nicknameLoadingFinished = true
                }
                .subscribeBy(
                        onSuccess = { email ->
                            ownerNickname = email
                            displayAccount()
                        },
                        onError = {
                            // Not critical.
                            displayAccount()
                        }
                )
                .addTo(compositeDisposable)
    }

    companion object {
        private const val BOOKING_EXTRA = "booking"
        private const val ACCOUNT_ITEM_ID = 1L

        fun getBundle(booking: BookingRecord) = Bundle().apply {
            putSerializable(BOOKING_EXTRA, booking)
        }
    }
}