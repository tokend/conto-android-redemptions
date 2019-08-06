package org.tokend.contoredemptions.features.history.data.model

import androidx.room.*
import org.tokend.contoredemptions.features.assets.data.model.Asset
import org.tokend.contoredemptions.features.assets.data.model.SimpleAsset
import org.tokend.sdk.factory.GsonFactory
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal
import java.util.*

@Entity(tableName = "redemption")
@TypeConverters(RedemptionDbEntity.Converters::class)
data class RedemptionDbEntity(
        @PrimaryKey
        @ColumnInfo(name = "uid")
        val uid: Long,
        @Embedded
        val sourceAccount: RedemptionRecord.Account,
        @ColumnInfo(name = "company_id", index = true)
        val companyId: String,
        @ColumnInfo(name = "company_name")
        val companyName: String,
        @ColumnInfo(name = "amount")
        val amount: BigDecimal,
        @ColumnInfo(name = "asset")
        val asset: Asset,
        @ColumnInfo(name = "date")
        val date: Date,
        @ColumnInfo(name = "reference", index = true)
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
            return value?.let { gson.toJson(it) }
        }

        @TypeConverter
        fun dateToUnix(value: Date?): Long? {
            return value?.let { it.time / 1000 }
        }

        @TypeConverter
        fun dateFromUnix(value: Long?): Date? {
            return value?.let { Date(it * 1000) }
        }

        @TypeConverter
        fun bigDecimalToString(value: BigDecimal?): String? {
            return value?.let { BigDecimalUtil.toPlainString(it) }
        }

        @TypeConverter
        fun stringToBigDecimal(value: String?): BigDecimal? {
            return value?.let { BigDecimalUtil.valueOf(it) }
        }
    }

    fun toRecord(): RedemptionRecord {
        return RedemptionRecord(
                sourceAccount = sourceAccount,
                company = RedemptionRecord.Company(
                        companyId,
                        companyName
                ),
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
                    date = record.date,
                    reference = record.reference,
                    asset = record.asset,
                    amount = record.amount,
                    companyId = record.company.id,
                    companyName = record.company.name
            )
        }
    }
}