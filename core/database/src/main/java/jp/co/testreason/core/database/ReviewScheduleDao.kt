package jp.co.testreason.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewScheduleDao {
    @Query("SELECT * FROM review_schedules WHERE nextReviewAt <= :now")
    fun getDueReviewSchedules(now: Long): Flow<List<ReviewScheduleEntity>>

    @Query("SELECT * FROM review_schedules WHERE questionId = :questionId")
    suspend fun getSchedule(questionId: String): ReviewScheduleEntity?

    @Query("SELECT * FROM review_schedules")
    fun getAllSchedules(): Flow<List<ReviewScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSchedule(schedule: ReviewScheduleEntity)
}
