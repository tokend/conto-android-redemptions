package org.tokend.contoredemptions.features.dashboard.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.dip
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.features.history.view.RedemptionsFragment
import org.tokend.contoredemptions.features.pos.view.PosTerminalFragment
import org.tokend.contoredemptions.features.scanner.view.ProcessRedeeemableFragment
import org.tokend.contoredemptions.util.Navigator
import org.tokend.contoredemptions.view.util.LogoUtil

class DashboardActivity : BaseActivity() {
    private var currentFragmentId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initToolbar()
        initTabs()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        title = ""

        toolbar.addView(
                ImageView(this).apply {
                    layoutParams = Toolbar.LayoutParams(
                            Toolbar.LayoutParams.WRAP_CONTENT,
                            dip(24)
                    ).apply {
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }

                    setImageDrawable(
                            ContextCompat.getDrawable(
                                    this@DashboardActivity,
                                    R.mipmap.product_logo)
                    )

                    scaleType = ImageView.ScaleType.FIT_START

                    setOnLongClickListener {
                        Navigator.from(this@DashboardActivity).openSettings()
                        true
                    }
                }
        )

        toolbar.addView(
                ImageView(this).apply {
                    val size = dip(32)

                    layoutParams = Toolbar.LayoutParams(size, size).apply {
                        gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        setMargins(0, 0,
                                context.resources.getDimensionPixelSize(R.dimen.standard_margin), 0)
                    }

                    scaleType = ImageView.ScaleType.CENTER_INSIDE

                    val c = companyProvider.getCompany()
                    LogoUtil.setLogo(this, c.name, c.logoUrl, size)

                    TooltipCompat.setTooltipText(this, getString(R.string.select_company_title))

                    setOnClickListener {
                        Navigator.from(this@DashboardActivity)
                                .openCompanies(COMPANY_SELECTION_REQUEST)
                    }
                }
        )

        toolbar.navigationIcon = null
    }

    private fun initTabs() {
        bottom_tabs.setOnNavigationItemSelectedListener {
            displayFragment(it.itemId)
            true
        }

        bottom_tabs.selectedItemId = R.id.scan

        val isNfcAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
        if (!isNfcAvailable) {
            bottom_tabs.menu.removeItem(R.id.terminal)
        }
    }

    private fun displayFragment(id: Int) {
        if (currentFragmentId == id) {
            return
        }

        val fragment = when (id) {
            R.id.scan -> ProcessRedeeemableFragment.newInstance()
            R.id.history -> RedemptionsFragment.newInstance()
            R.id.terminal -> PosTerminalFragment.newInstance()
            else -> null
        }

        if (fragment != null) {
            currentFragmentId = id
            displayFragment(fragment)
        } else {
            Log.e("Dashboard", "Unknown screen ID")
        }
    }

    private fun displayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .disallowAddToBackStack()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                COMPANY_SELECTION_REQUEST -> finish()
            }
        }
    }

    companion object {
        private val COMPANY_SELECTION_REQUEST = "select_company".hashCode() and 0xffff
    }
}
