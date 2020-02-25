package org.tokend.contoredemptions.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.newTask
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.features.booking.model.BookingRecord
import org.tokend.contoredemptions.features.booking.view.BookingDetailsActivity
import org.tokend.contoredemptions.features.companies.view.CompaniesActivity
import org.tokend.contoredemptions.features.dashboard.view.DashboardActivity
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.features.history.view.RedemptionDetailsActivity
import org.tokend.contoredemptions.features.redemption.view.ConfirmRedemptionActivity
import org.tokend.contoredemptions.features.settings.view.SettingsActivity
import org.tokend.contoredemptions.features.urlconfig.view.ChangeNetworkActivity

/**
 * Performs transitions between screens.
 * 'open-' will open related screen as a child.<p>
 * 'to-' will open related screen and finish current.
 */
class Navigator private constructor() {
    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var context: Context? = null

    companion object {
        fun from(activity: Activity): Navigator {
            val navigator = Navigator()
            navigator.activity = activity
            navigator.context = activity
            return navigator
        }

        fun from(fragment: Fragment): Navigator {
            val navigator = Navigator()
            navigator.fragment = fragment
            navigator.context = fragment.requireContext()
            return navigator
        }

        fun from(context: Context): Navigator {
            val navigator = Navigator()
            navigator.context = context
            return navigator
        }
    }

    private fun performIntent(intent: Intent?, requestCode: Int? = null, bundle: Bundle? = null) {
        if (intent != null) {
            if (!IntentLock.checkIntent(intent, context)) return
            activity?.let {
                if (requestCode != null) {
                    it.startActivityForResult(intent, requestCode, bundle ?: Bundle.EMPTY)
                } else {
                    it.startActivity(intent, bundle ?: Bundle.EMPTY)
                }
                return
            }

            fragment?.let {
                if (requestCode != null) {
                    it.startActivityForResult(intent, requestCode, bundle ?: Bundle.EMPTY)
                } else {
                    it.startActivity(intent, bundle ?: Bundle.EMPTY)
                }
                return
            }

            context?.startActivity(intent.newTask(), bundle ?: Bundle.EMPTY)
        }
    }

    private fun fadeOut(activity: Activity) {
        ActivityCompat.finishAfterTransition(activity)
        activity.overridePendingTransition(0, R.anim.activity_fade_out)
        activity.finish()
    }

    private fun finishAffinity(activity: Activity) {
        activity.setResult(Activity.RESULT_CANCELED, null)
        ActivityCompat.finishAffinity(activity)
    }

    private fun createTransitionBundle(activity: Activity, vararg pairs: Pair<View?, String>): Bundle {
        val sharedViews = arrayListOf<androidx.core.util.Pair<View, String>>()

        pairs.forEach {
            val view = it.first
            if (view != null) {
                sharedViews.add(androidx.core.util.Pair(view, it.second))
            }
        }

        return if (sharedViews.isEmpty()) {
            Bundle.EMPTY
        } else {
            ActivityOptionsCompat.makeSceneTransitionAnimation(activity,
                    *sharedViews.toTypedArray()).toBundle() ?: Bundle.EMPTY
        }
    }

    fun toDashboard() {
        context?.intentFor<DashboardActivity>()
                ?.also {
                    performIntent(it)
                    activity?.finish()
                }
    }

    fun openCompanies(requestCode: Int = 0) {
        context?.intentFor<CompaniesActivity>()
                ?.putExtras(CompaniesActivity.getBundle(canGoBack = true, forceCompanyLoad = false))
                ?.also { performIntent(it, requestCode = requestCode) }
    }

    fun openAcceptRedemption(redemptionRequest: ByteArray,
                             requestCode: Int = 0) {
        context?.intentFor<ConfirmRedemptionActivity>()
                ?.putExtras(ConfirmRedemptionActivity.getBundle(redemptionRequest))
                ?.also { performIntent(it, requestCode = requestCode) }
    }

    fun openRedemptionDetails(redemption: RedemptionRecord) {
        context?.intentFor<RedemptionDetailsActivity>()
                ?.putExtras(RedemptionDetailsActivity.getBundle(redemption))
                ?.also { performIntent(it) }
    }

    fun openBookingDetails(bookingRecord: BookingRecord,
                           requestCode: Int = 0) {
        context?.intentFor<BookingDetailsActivity>()
                ?.putExtras(BookingDetailsActivity.getBundle(bookingRecord))
                ?.also { performIntent(it, requestCode = requestCode) }
    }

    fun openSettings() {
        context?.intentFor<SettingsActivity>()
                ?.also { performIntent(it) }
    }

    fun openNetworkChange(requestCode: Int = 0) {
        context?.intentFor<ChangeNetworkActivity>()
                ?.also { performIntent(it, requestCode) }
    }
}
