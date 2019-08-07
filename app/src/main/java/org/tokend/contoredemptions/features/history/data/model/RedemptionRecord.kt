package org.tokend.contoredemptions.features.history.data.model

import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.companies.data.model.CompanyRecord
import org.tokend.contoredemptions.util.LongUid
import java.io.Serializable
import java.math.BigDecimal
import java.util.*

/**
 * Holds accepted redemption request data
 */
class RedemptionRecord(
        val sourceAccount: Account,
        val company: Company,
        val asset: Asset,
        val amount: BigDecimal,
        val date: Date,
        val reference: Long,
        val id: Long = LongUid.get()
) : Serializable {
    data class Account(
            val id: String,
            val nickname: String?
    ) : Serializable

    data class Company(
            val id: String,
            val name: String
    ) : Serializable {
        constructor(source: CompanyRecord) : this(
                id = source.id,
                name = source.name
        )
    }

    override fun equals(other: Any?): Boolean {
        return other is RedemptionRecord && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}