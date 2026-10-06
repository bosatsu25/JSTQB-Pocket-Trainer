package jp.co.testreason.core.model

data class Mastery(
    val learningObjectiveId: String,
    val score: Float,
    val lastAttemptAt: Long,
    val totalAttempts: Int,
    val correctAttempts: Int
)
