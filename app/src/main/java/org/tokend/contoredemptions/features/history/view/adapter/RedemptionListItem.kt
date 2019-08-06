package org.tokend.contoredemptions.features.history.view.adapter

import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.history.data.model.RedemptionRecord
import org.tokend.contoredemptions.util.DateProvider
import org.tokend.contoredemptions.util.formatter.AccountIdFormatter
import java.math.BigDecimal
import java.util.*

class RedemptionListItem(
        val asset: Asset,
        val account: String,
        override val date: Date,
        val amount: BigDecimal,
        val source: RedemptionRecord?
): DateProvider {
    constructor(source: RedemptionRecord,
                accountIdFormatter: AccountIdFormatter) : this(
            asset = source.asset,
            account = source.sourceAccount.nickname
                    ?: accountIdFormatter.formatShort(source.sourceAccount.id),
            date = source.date,
            amount = source.amount,
            source = source
    )
}