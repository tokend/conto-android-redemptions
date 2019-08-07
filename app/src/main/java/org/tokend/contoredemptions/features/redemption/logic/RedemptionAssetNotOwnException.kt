package org.tokend.contoredemptions.features.redemption.logic

import org.tokend.contoredemptions.features.assets.data.model.Asset

class RedemptionAssetNotOwnException(val asset: Asset): Exception()