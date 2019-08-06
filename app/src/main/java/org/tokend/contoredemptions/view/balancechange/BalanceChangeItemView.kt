package org.tokend.contoredemptions.view.balancechange

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import org.tokend.contoredemptions.features.assets.data.model.Asset
import java.math.BigDecimal
import java.util.*

interface BalanceChangeItemView {
    val iconImageView: ImageView
    val mainInfoTextView: TextView
    val extraInfoTextView: TextView
    val amountTextView: TextView
    val dateTextView: TextView
    val dividerView: View

    var dividerIsVisible: Boolean

    fun displayAmount(amount: BigDecimal, asset: Asset)

    fun displayDate(date: Date)

    fun setIcon(iconDrawable: Drawable)

    fun setIcon(@DrawableRes iconRes: Int)

    fun displayMainInfo(mainInfo: String)

    fun displayExtraInfo(extraInfo: String?)
}