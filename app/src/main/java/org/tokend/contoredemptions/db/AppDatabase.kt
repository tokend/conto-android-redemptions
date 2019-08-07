package org.tokend.contoredemptions.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.tokend.contoredemptions.features.history.data.model.RedemptionDbEntity
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsDao
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal
import java.util.*

@Database(
        entities = [
            RedemptionDbEntity::class
        ],
        version = 1,
        exportSchema = false
)
@TypeConverters(AppDatabase.Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val redemptionsDao: RedemptionsDao

    class Converters {
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
}