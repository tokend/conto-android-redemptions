package org.tokend.contoredemptions.view.util

import androidx.fragment.app.Fragment
import org.tokend.contoredemptions.features.redemption.view.ScanRedemptionFragment

class FragmentFactory {

    fun getScanRedemptionFragment(): Fragment {
        return ScanRedemptionFragment()
    }

    fun getHistoryFragment(): Fragment {
        return Fragment()
    }
}