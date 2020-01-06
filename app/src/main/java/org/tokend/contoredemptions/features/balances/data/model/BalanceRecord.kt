package org.tokend.contoredemptions.features.balances.data.model

import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.assets.data.model.SimpleAsset
import org.tokend.sdk.api.generated.resources.BalanceResource

class BalanceRecord(
        val id: String,
        val asset: Asset
) {
    constructor(source: BalanceResource): this(
            id = source.id,
            asset = SimpleAsset(source.asset)
    )
}