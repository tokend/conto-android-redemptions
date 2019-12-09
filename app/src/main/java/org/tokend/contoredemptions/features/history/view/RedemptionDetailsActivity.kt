package org.tokend.contoredemptions.features.history.view

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import kotlinx.android.synthetic.main.activity_balance_change_details.*
import kotlinx.android.synthetic.main.appbar_with_balance_change_main_data.*
import kotlinx.android.synthetic.main.toolbar.*
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.BaseActivity
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.util.formatter.AccountIdFormatter
import org.tokend.contoredemptions.view.balancechange.BalanceChangeMainDataView
import org.tokend.contoredemptions.view.details.DetailsItem
import org.tokend.contoredemptions.view.details.adapter.DetailsItemsAdapter

class RedemptionDetailsActivity : BaseActivity() {
    protected val adapter = DetailsItemsAdapter()
    protected lateinit var mainDataView: BalanceChangeMainDataView

    private lateinit var redemption: RedemptionRecord

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance_change_details)

        val redemption = intent.getSerializableExtra(REDEMPTION_EXTRA) as? RedemptionRecord
        if (redemption == null) {
            errorHandlerFactory.getDefault().handle(
                    IllegalArgumentException(
                            "No $REDEMPTION_EXTRA specified"
                    )
            )
            finish()
            return
        }
        this.redemption = redemption

        initToolbar()
        initMainDataView()
        initList()

        displayDetails()
    }

    private fun initToolbar() {
        toolbar.background = ColorDrawable(Color.WHITE)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initMainDataView() {
        mainDataView = BalanceChangeMainDataView(appbar, amountFormatter, dateFormatter)
    }

    private fun initList() {
        details_list.layoutManager = LinearLayoutManager(this)
        details_list.adapter = adapter
        (details_list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    private fun displayDetails() {
        mainDataView.displayAmount(
                redemption.amount,
                redemption.asset,
                null
        )
        mainDataView.displayOperationName(getString(R.string.operation_redemption))
        mainDataView.displayDate(redemption.date)

        displayAccount()
        displayCompany()
    }

    private fun displayAccount() {
        val accountIdFormatter = AccountIdFormatter()

        adapter.addData(
                DetailsItem(
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_account),
                        hint = getString(R.string.account),
                        text = redemption.sourceAccount.nickname
                                ?: accountIdFormatter.formatShort(redemption.sourceAccount.id)
                )
        )
    }

    private fun displayCompany() {
        adapter.addData(
                DetailsItem(
                        icon = ContextCompat.getDrawable(this, R.drawable.ic_briefcase),
                        hint = getString(R.string.redemption_company),
                        text = redemption.company.name
                )
        )
    }

    companion object {
        private const val REDEMPTION_EXTRA = "redemption"

        fun getBundle(redemption: RedemptionRecord) = Bundle().apply {
            putSerializable(REDEMPTION_EXTRA, redemption)
        }
    }
}