package org.tokend.contoredemptions.features.balances.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.tokend.contoredemptions.features.assets.data.model.AssetRecord
import org.tokend.contoredemptions.util.UrlConfig
import org.tokend.sdk.api.generated.resources.BalanceResource

class BalanceRecord(
        val id: String,
        val asset: AssetRecord
) {
    constructor(source: BalanceResource, urlConfig: UrlConfig?, mapper: ObjectMapper): this(
            id = source.id,
            asset = AssetRecord.fromResource(source.asset, urlConfig, mapper)
    )
}