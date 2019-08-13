package org.tokend.contoredemptions.features.assets.data.model

import com.fasterxml.jackson.databind.node.NullNode
import com.google.gson.annotations.SerializedName
import org.tokend.sdk.api.generated.resources.AssetResource

class SimpleAsset(@SerializedName("code")
                  override val code: String,
                  @SerializedName("trailing_digits")
                  override val trailingDigits: Int,
                  @SerializedName("name")
                  override val name: String?
) : Asset {

    constructor(source: AssetResource) : this(
            code = source.id,
            trailingDigits = source.trailingDigits.toInt(),
            name = source.details.get("name")?.takeIf { it !is NullNode }?.asText()
    )

    constructor(source: Asset) : this(
            code = source.code,
            trailingDigits = source.trailingDigits,
            name = source.name
    )

    @Deprecated("Going to be removed. Right now used because of some issues")
    constructor(asset: String) : this(
            code = asset,
            trailingDigits = 6,
            name = null
    )

    override fun equals(other: Any?): Boolean {
        return other is SimpleAsset && other.code == this.code
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    override fun toString(): String {
        return code
    }
}