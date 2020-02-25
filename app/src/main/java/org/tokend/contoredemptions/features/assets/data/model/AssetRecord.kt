package org.tokend.contoredemptions.features.assets.data.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import org.tokend.contoredemptions.util.PolicyChecker
import org.tokend.contoredemptions.features.urlconfig.model.UrlConfig
import org.tokend.sdk.api.base.model.RemoteFile
import org.tokend.sdk.api.generated.resources.AssetResource
import org.tokend.sdk.utils.HashCodes
import java.io.Serializable
import java.math.BigDecimal

class AssetRecord(
        override val code: String,
        val policy: Int,
        override val name: String?,
        val description: String?,
        val logoUrl: String?,
        val terms: RemoteFile?,
        val externalSystemType: Int?,
        val issued: BigDecimal?,
        val available: BigDecimal?,
        val maximum: BigDecimal,
        val ownerAccountId: String,
        override val trailingDigits: Int,
        val localizedNames: Map<String, String>
) : Serializable, PolicyChecker, Asset {
    val isBackedByExternalSystem: Boolean
        get() = externalSystemType != null

    val isTransferable: Boolean
        get() = checkPolicy(policy, org.tokend.wallet.xdr.AssetPolicy.TRANSFERABLE.value)

    val isWithdrawable: Boolean
        get() = checkPolicy(policy, org.tokend.wallet.xdr.AssetPolicy.WITHDRAWABLE.value)

    val canBeBaseForAtomicSwap: Boolean
        get() = checkPolicy(policy, org.tokend.wallet.xdr.AssetPolicy.CAN_BE_BASE_IN_ATOMIC_SWAP.value)

    val isBase: Boolean
        get() = checkPolicy(policy, org.tokend.wallet.xdr.AssetPolicy.BASE_ASSET.value)

    override fun equals(other: Any?): Boolean {
        return other is AssetRecord
                && other.code == this.code
                && other.logoUrl == this.logoUrl
    }

    override fun hashCode(): Int {
        return HashCodes.ofMany(code, logoUrl)
    }

    override fun toString(): String {
        return code
    }

    companion object {
        @JvmStatic
        fun fromResource(source: AssetResource, urlConfig: UrlConfig?, mapper: ObjectMapper): AssetRecord {

            val name = source.details.get("name")?.takeIf { it !is NullNode }?.asText()
            val localizedNames = try {
                mapper.readTree(name)
                        .fields()
                        .asSequence()
                        .map { it.key to it.value.asText() }
                        .toMap()
            } catch (_: Exception) {
                emptyMap<String, String>()
            }

            val logo = source.details.get("logo")?.takeIf { it !is NullNode }?.let {
                mapper.convertValue(it, RemoteFile::class.java)
            }

            val terms = source.details.get("terms")?.takeIf { it !is NullNode }?.let {
                mapper.convertValue(it, RemoteFile::class.java)
            }

            val externalSystemType =
                    source.details.get("external_system_type")
                            ?.asText("")
                            ?.takeIf { it.isNotEmpty() }
                            ?.toIntOrNull()

            val description = source.details.get("description")
                    ?.asText("")
                    ?.takeIf(String::isNotEmpty)

            return AssetRecord(
                    code = source.id,
                    policy = source.policies.value
                            ?: throw IllegalStateException("Asset must have a policy"),
                    name = name,
                    description = description,
                    logoUrl = logo?.getUrl(urlConfig?.storage),
                    terms = terms,
                    externalSystemType = externalSystemType,
                    issued = source.issued,
                    available = source.availableForIssuance,
                    maximum = source.maxIssuanceAmount,
                    ownerAccountId = source.owner.id,
                    trailingDigits = source.trailingDigits.toInt(),
                    localizedNames = localizedNames
            )
        }
    }
}