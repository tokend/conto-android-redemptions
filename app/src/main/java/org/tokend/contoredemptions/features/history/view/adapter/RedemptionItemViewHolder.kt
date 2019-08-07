package org.tokend.contoredemptions.features.history.view.adapter

import android.view.View
import androidx.core.content.ContextCompat
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.adapter.BaseViewHolder
import org.tokend.contoredemptions.util.formatter.AmountFormatter
import org.tokend.contoredemptions.util.formatter.DateFormatter
import org.tokend.contoredemptions.view.balancechange.BalanceChangeItemView
import org.tokend.contoredemptions.view.balancechange.BalanceChangeItemViewImpl

class RedemptionItemViewHolder(
        view: View,
        amountFormatter: AmountFormatter,
        dateFormatter: DateFormatter
) : BaseViewHolder<RedemptionListItem>(view),
        BalanceChangeItemView by BalanceChangeItemViewImpl(view, amountFormatter, dateFormatter) {

    private val icon = ContextCompat.getDrawable(view.context, R.drawable.ic_sent)!!

    override fun bind(item: RedemptionListItem) {
        displayMainInfo(item.asset.name ?: item.asset.code)
        displayExtraInfo(
                view.context.getString(
                        R.string.template_balance_change_by_account,
                        item.account
                )
        )
        displayAmount(item.amount, item.asset)
        displayDate(item.date)
        setIcon(icon)
    }
}