package org.tokend.contoredemptions.features.dashboard.view

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.toolbar.*
import org.jetbrains.anko.dip
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity

class DashboardActivity : BaseActivity() {

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
                        gravity = Gravity.CENTER
                    }

                    setImageDrawable(
                            ContextCompat.getDrawable(
                                    this@DashboardActivity,
                                    R.mipmap.product_logo)
                    )

                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
        )
    }

    private fun initTabs() {
        bottom_tabs.setOnNavigationItemSelectedListener {
            displayFragment(it.itemId)
            true
        }

        bottom_tabs.selectedItemId = R.id.scan
    }

    private fun displayFragment(id: Int) {
        // TODO: Use real fragments
        when (id) {
            R.id.scan -> displayFragment(Fragment())
            R.id.history -> displayFragment(Fragment())
            else -> Log.e("Dashboard", "Unknown screen ID")
        }
    }

    private fun displayFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
                .disallowAddToBackStack()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragment_container, fragment)
                .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.dashboard, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
