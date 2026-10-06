package jp.co.testreason.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "masteries")
data class MasteryEntity(
    @PrimaryKey val learningObjectiveId: String,
    val score: Float,
    val lastAttemptAt: Long,
    val totalAttempts: Int,
    val correctAttempts: Int
)
