package org.tokend.contoredemptions.features.history.view.adapter

import android.view.View
import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.adapter.BaseViewHolder
import org.tokend.contoredemptions.base.view.adapter.PaginationRecyclerAdapter
import org.tokend.contoredemptions.util.formatter.AmountFormatter
import org.tokend.contoredemptions.util.formatter.DateFormatter

class RedemptionsAdapter(
        private val amountFormatter: AmountFormatter,
        private val dateFormatter: DateFormatter
) : PaginationRecyclerAdapter<RedemptionListItem, BaseViewHolder<RedemptionListItem>>() {
    private class FooterViewHolder(view: View) : BaseViewHolder<RedemptionListItem>(view) {
        override fun bind(item: RedemptionListItem) {}
    }

    override fun createFooterViewHolder(parent: ViewGroup): BaseViewHolder<RedemptionListItem> {
        return FooterViewHolder(
                parent.context.layoutInflater.inflate(
                        R.layout.list_item_loading_footer,
                        parent,
                        false
                )
        )
    }

    override fun bindFooterViewHolder(holder: BaseViewHolder<RedemptionListItem>) {}

    override fun createItemViewHolder(parent: ViewGroup): BaseViewHolder<RedemptionListItem> {
        return RedemptionItemViewHolder(
                parent.context.layoutInflater.inflate(
                        R.layout.list_item_balance_change,
                        parent,
                        false
                ),
                amountFormatter,
                dateFormatter
        )
    }

    override fun bindItemViewHolder(holder: BaseViewHolder<RedemptionListItem>, position: Int) {
        super.bindItemViewHolder(holder, position)
        val isLastInSection =
                position == itemCount - (if (needLoadingFooter) 2 else 1)
        (holder as? RedemptionItemViewHolder)?.dividerIsVisible = !isLastInSection
    }
}