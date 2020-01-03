package org.tokend.contoredemptions.features.pos.view

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_disabled_nfc.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment

class NfcDisabledFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_disabled_nfc, container, false)
    }

    override fun onInitAllowed() {
        open_settings_button.setOnClickListener {
            openNfcSettings()
        }
    }

    private fun openNfcSettings() {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS));
    }

    companion object {
        fun newInstance() = NfcDisabledFragment()
    }
}