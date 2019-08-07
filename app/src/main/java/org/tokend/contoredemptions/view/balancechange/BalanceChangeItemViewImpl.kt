package org.tokend.contoredemptions.view.balancechange

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.list_item_balance_change.view.*
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.util.formatter.AmountFormatter
import org.tokend.contoredemptions.util.formatter.DateFormatter
import java.math.BigDecimal
import java.util.*

class BalanceChangeItemViewImpl(
        view: View,
        private val amountFormatter: AmountFormatter,
        private val dateFormatter: DateFormatter
) : BalanceChangeItemView {
    override val iconImageView: ImageView = view.icon_image_view
    override val mainInfoTextView: TextView = view.main_info_text_view
    override val extraInfoTextView: TextView = view.extra_info_text_view
    override val amountTextView: TextView = view.amount_text_view
    override val dateTextView: TextView = view.date_text_view
    override val dividerView: View = view.divider_view

    override var dividerIsVisible: Boolean
        get() = dividerView.isVisible
        set(value) {
            dividerView.isVisible = value
        }

    override fun displayAmount(amount: BigDecimal, asset: Asset) {
        amountTextView.text = amountFormatter.formatAssetAmount(
                amount, asset, withAssetCode = false
        )
    }

    override fun displayDate(date: Date) {
        dateTextView.text = dateFormatter.formatTimeOrDate(date)
    }

    override fun setIcon(iconDrawable: Drawable) {
        iconImageView.setImageDrawable(iconDrawable)
    }

    override fun setIcon(iconRes: Int) {
        ContextCompat.getDrawable(iconImageView.context, iconRes)
                ?.also(this::setIcon)
    }

    override fun displayMainInfo(mainInfo: String) {
        mainInfoTextView.text = mainInfo
    }

    override fun displayExtraInfo(extraInfo: String?) {
        extraInfoTextView.text = extraInfo
        extraInfoTextView.isVisible = extraInfo != null
    }
}