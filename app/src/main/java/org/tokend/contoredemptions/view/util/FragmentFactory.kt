package org.tokend.contoredemptions.view.util

import androidx.fragment.app.Fragment
import org.tokend.contoredemptions.features.history.view.RedemptionsFragment
import org.tokend.contoredemptions.features.scanner.view.ProcessRedeeemableFragment

class FragmentFactory {

    fun getProcessRedeemableFragment(): Fragment {
        return ProcessRedeeemableFragment()
    }

    fun getHistoryFragment(): Fragment {
        return RedemptionsFragment()
    }
}