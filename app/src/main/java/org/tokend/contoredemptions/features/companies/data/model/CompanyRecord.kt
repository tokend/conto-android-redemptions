package org.tokend.contoredemptions.features.companies.data.model

import com.google.gson.annotations.SerializedName
import org.tokend.contoredemptions.util.UrlConfig
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.integrations.dns.model.BusinessResource
import org.tokend.sdk.factory.GsonFactory
import java.io.Serializable

class CompanyRecord(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("industry")
    val industry: String?,
    @SerializedName("logo_url")
    val logoUrl: String?,
    @SerializedName("conversion_asset_code")
    val conversionAssetCode: String?
) : Serializable {
    constructor(source: BusinessResource, urlConfig: UrlConfig?) : this(
        id = source.accountId,
        name = source.name.takeIf(String::isNotEmpty)
            ?: throw IllegalArgumentException("Company name can't be empty"),
        industry = source.industry.takeIf(String::isNotEmpty),
        logoUrl = source.logoJson
            .let { GsonFactory().getBaseGson().fromJson(it, RemoteFile::class.java) }
            .getUrl(urlConfig?.storage),
        conversionAssetCode = source.statsQuoteAsset.takeIf(String::isNotEmpty)
    )

    override fun equals(other: Any?): Boolean {
        return other is CompanyRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}