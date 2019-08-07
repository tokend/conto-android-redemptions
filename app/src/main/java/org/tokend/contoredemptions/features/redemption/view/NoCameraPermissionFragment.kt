package org.tokend.contoredemptions.features.redemption.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.Completable
import io.reactivex.subjects.CompletableSubject
import kotlinx.android.synthetic.main.fragment_no_camera_permission.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseFragment

class NoCameraPermissionFragment : BaseFragment() {
    private val buttonClickSubject = CompletableSubject.create()
    val retryButtonClick: Completable = buttonClickSubject

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_no_camera_permission, container, false)
    }

    override fun onInitAllowed() {
        initButtons()
    }

    private fun initButtons() {
        retry_button.setOnClickListener {
            buttonClickSubject.onComplete()
        }

        open_settings_button.setOnClickListener {
            openSystemAppSettings()
        }
    }

    private fun openSystemAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", requireContext().packageName, null))
        )
    }
}