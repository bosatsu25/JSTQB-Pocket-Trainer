package jp.co.testreason.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MasteryDao {
    @Query("SELECT * FROM masteries")
    fun getAllMasteries(): Flow<List<MasteryEntity>>

    @Query("SELECT * FROM masteries WHERE learningObjectiveId = :loId")
    suspend fun getMasteryByLo(loId: String): MasteryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMastery(mastery: MasteryEntity)
}
