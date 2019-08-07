package org.tokend.contoredemptions.features.redemption.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment

class LoadingFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_loading_data, container, false)
    }

    override fun onInitAllowed() {}
}