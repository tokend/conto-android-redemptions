package org.tokend.contoredemptions.features.history.data.model

import androidx.room.*
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.assets.data.model.SimpleAsset
import org.tokend.sdk.factory.GsonFactory
import java.math.BigDecimal
import java.util.*

@Entity(
        tableName = "redemption",
        indices = [
            Index("company_id"),
            Index("reference", unique = true)
        ]
)
@TypeConverters(RedemptionDbEntity.Converters::class)
data class RedemptionDbEntity(
        @PrimaryKey
        @ColumnInfo(name = "uid")
        val uid: Long,
        @Embedded(prefix = "account_")
        val sourceAccount: RedemptionRecord.Account,
        @Embedded(prefix = "company_")
        val company: RedemptionRecord.Company,
        @ColumnInfo(name = "amount")
        val amount: BigDecimal,
        @ColumnInfo(name = "asset")
        val asset: Asset,
        @ColumnInfo(name = "date")
        val date: Date,
        @ColumnInfo(name = "reference")
        val reference: Long
) {
    class Converters {
        private val gson = GsonFactory().getBaseGson()

        @TypeConverter
        fun assetFromJson(value: String?): Asset? {
            return value?.let { gson.fromJson(value, SimpleAsset::class.java) }
        }

        @TypeConverter
        fun assetToJson(value: Asset?): String? {
            return value?.let { gson.toJson(SimpleAsset(it)) }
        }
    }

    fun toRecord(): RedemptionRecord {
        return RedemptionRecord(
                sourceAccount = sourceAccount,
                company = company,
                amount = amount,
                asset = asset,
                reference = reference,
                id = uid,
                date = date
        )
    }

    companion object {
        fun fromRecord(record: RedemptionRecord): RedemptionDbEntity {
            return RedemptionDbEntity(
                    uid = record.id,
                    sourceAccount = record.sourceAccount,
                    company = record.company,
                    date = record.date,
                    reference = record.reference,
                    asset = record.asset,
                    amount = record.amount
            )
        }
    }
}