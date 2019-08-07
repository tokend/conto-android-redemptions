package org.tokend.contoredemptions.view.util

import androidx.fragment.app.Fragment
import org.tokend.contoredemptions.features.history.view.RedemptionsFragment
import org.tokend.contoredemptions.features.redemption.view.ProcessRedemptionFragment

class FragmentFactory {

    fun getProcessRedemptionFragment(): Fragment {
        return ProcessRedemptionFragment()
    }

    fun getHistoryFragment(): Fragment {
        return RedemptionsFragment()
    }
}