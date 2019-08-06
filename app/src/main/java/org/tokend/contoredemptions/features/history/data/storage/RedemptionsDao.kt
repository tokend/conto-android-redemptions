package org.tokend.contoredemptions.features.history.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.tokend.contoredemptions.features.history.data.model.RedemptionDbEntity

@Dao
interface RedemptionsDao {
    @Query("SELECT * FROM redemption WHERE company_id=:companyId AND uid<:cursor ORDER BY uid DESC LIMIT :count ")
    fun getPageDesc(companyId: String,
                    count: Int,
                    cursor: Long): List<RedemptionDbEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg items: RedemptionDbEntity)
}