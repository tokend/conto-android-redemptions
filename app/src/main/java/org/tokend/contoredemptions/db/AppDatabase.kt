package org.tokend.contoredemptions.db

import androidx.room.Database
import androidx.room.RoomDatabase
import org.tokend.contoredemptions.features.history.data.model.RedemptionDbEntity
import org.tokend.contoredemptions.features.history.data.storage.RedemptionsDao

@Database(
        entities = [
            RedemptionDbEntity::class
        ],
        version = 1,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val redemptionsDao: RedemptionsDao
}