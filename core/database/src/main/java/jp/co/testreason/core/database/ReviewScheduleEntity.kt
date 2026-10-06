package jp.co.testreason.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "review_schedules")
data class ReviewScheduleEntity(
    @PrimaryKey val questionId: String,
    val nextReviewAt: Long,
    val lastAttemptAt: Long,
    val intervalDays: Int
)
