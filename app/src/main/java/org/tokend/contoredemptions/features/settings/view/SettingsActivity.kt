package org.tokend.contoredemptions.features.settings.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.include_appbar_elevation.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.util.Navigator
import org.tokend.contoredemptions.view.details.DetailsItem
import org.tokend.contoredemptions.view.details.adapter.DetailsItemsAdapter
import org.tokend.contoredemptions.view.util.ElevationUtil

class SettingsActivity : BaseActivity() {
    private val adapter = DetailsItemsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initToolbar()
        initList()

        displayItems()
    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle(R.string.settings)
    }

    private fun initList() {
        items_list.layoutManager = LinearLayoutManager(this)
        items_list.adapter = adapter
        (items_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        ElevationUtil.initScrollElevation(items_list, appbar_elevation_view)

        adapter.onItemClick { _, item ->
            when (item.id) {
                URL_CONFIG_ITEM_ID ->
                    Navigator.from(this).openNetworkChange(CHANGE_NETWORK_REQUEST)
            }
        }
    }

    private fun displayItems() {
        adapter.clearData()
        displayUrlConfigItem()
    }

    private fun displayUrlConfigItem() {
        adapter.addData(DetailsItem(
                id = URL_CONFIG_ITEM_ID,
                hint = getString(R.string.app_network),
                icon = ContextCompat.getDrawable(this, R.drawable.ic_cloud),
                text = urlConfigProvider.getConfig().clientDomain
        ))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHANGE_NETWORK_REQUEST && resultCode == Activity.RESULT_OK) {
            restartApp()
        }
    }

    private fun restartApp() {
        packageManager
                ?.getLaunchIntentForPackage(packageName)
                ?.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.also {
                    startActivity(it)
                    ActivityCompat.finishAffinity(this)
                }
    }

    private companion object {
        private val URL_CONFIG_ITEM_ID = "url_config".hashCode().toLong()
        private val CHANGE_NETWORK_REQUEST = "change_network".hashCode() and 0xffff
    }
}