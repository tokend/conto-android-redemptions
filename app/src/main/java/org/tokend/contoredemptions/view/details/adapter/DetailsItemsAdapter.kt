package org.tokend.contoredemptions.view.details.adapter

import android.view.ViewGroup
import org.jetbrains.anko.layoutInflater
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.adapter.BaseRecyclerAdapter
import org.tokend.contoredemptions.view.details.DetailsItem

class DetailsItemsAdapter : BaseRecyclerAdapter<DetailsItem, DetailsItemViewHolder>() {
    override fun createItemViewHolder(parent: ViewGroup): DetailsItemViewHolder {
        val view = parent.context.layoutInflater.inflate(R.layout.list_item_details_row, parent, false)
        return DetailsItemViewHolder(view)
    }

    override fun bindItemViewHolder(holder: DetailsItemViewHolder, position: Int) {
        super.bindItemViewHolder(holder, position)
        val lastInGroup = position == itemCount - 1
                || getItemAt(position + 1)?.hasHeader == true
        val nextHasNoIcon = position == itemCount - 1
                || getItemAt(position + 1)?.icon == null

        holder.dividerIsVisible = !lastInGroup && !nextHasNoIcon
    }

    fun addOrUpdateItem(newItem: DetailsItem) {
        val index = items.indexOf(newItem)
        if (index > -1) {
            items[index] = newItem
            notifyItemChanged(index)
        } else {
            addData(newItem)
        }
    }
}