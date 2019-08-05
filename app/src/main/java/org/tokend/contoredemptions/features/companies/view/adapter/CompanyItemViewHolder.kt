package org.tokend.contoredemptions.features.companies.view.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.tokend.contoredemptions.R
import org.tokend.contoredemptions.base.view.adapter.BaseViewHolder
import org.tokend.contoredemptions.view.util.LogoUtil

class CompanyItemViewHolder(view: View) : BaseViewHolder<CompanyListItem>(view) {

    private val logoImageView = view.findViewById<ImageView>(R.id.company_logo_image_view)
    private val nameTextView = view.findViewById<TextView>(R.id.company_name_text_view)
    private val dividerView = view.findViewById<View>(R.id.divider_view)
    private val industryTextView = view.findViewById<TextView>(R.id.company_industry_text_view)

    private val baseLogoSize: Int =
        view.context.resources.getDimensionPixelSize(R.dimen.list_item_logo_size)

    var dividerIsVisible: Boolean
        get() = dividerView.visibility == View.VISIBLE
        set(value) {
            dividerView.visibility = if (value) View.VISIBLE else View.GONE
        }

    override fun bind(item: CompanyListItem) {
        nameTextView.text = item.name

        industryTextView.apply {
            visibility = item.industry?.let {
                text = it
                View.VISIBLE
            } ?: View.GONE
        }

        LogoUtil.setLogo(logoImageView, item.name,
            item.logoUrl, baseLogoSize)
    }
}