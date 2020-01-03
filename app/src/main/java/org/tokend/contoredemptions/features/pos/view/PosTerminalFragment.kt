package org.tokend.contoredemptions.features.pos.view

import android.nfc.NfcAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment
import org.tokend.contoredemptions.view.util.UserFlowFragmentDisplayer

class PosTerminalFragment : BaseFragment() {
    private val fragmentDisplayer =
            UserFlowFragmentDisplayer(this, R.id.fragment_container)

    private val isNfcEnabled: Boolean
        get() {
            return NfcAdapter.getDefaultAdapter(requireContext())?.isEnabled == true
        }
    private var wasNfcEnabled: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_fragment_container, container, false)
    }

    override fun onInitAllowed() {
        // Start screen selection is in onResume.
    }

    private fun toNfcDisabledError() {
        fragmentDisplayer.display(NfcDisabledFragment.newInstance(), "nfc_disabled", true)
    }

    private fun toPaymentAccepting() {
        fragmentDisplayer.display(AcceptPosPaymentFragment.newInstance(), "payment", true)
    }

    override fun onResume() {
        super.onResume()

        if (!wasNfcEnabled && isNfcEnabled) {
            toPaymentAccepting()
        } else if (!wasNfcEnabled && !isNfcEnabled) {
            toNfcDisabledError()
        }
    }

    override fun onPause() {
        super.onPause()
        wasNfcEnabled = isNfcEnabled
    }

    companion object {
        fun newInstance() = PosTerminalFragment()
    }
}