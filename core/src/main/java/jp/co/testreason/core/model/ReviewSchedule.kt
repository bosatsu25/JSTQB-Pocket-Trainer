package jp.co.testreason.core.model

data class ReviewSchedule(
    val questionId: String,
    val nextReviewAt: Long,
    val lastAttemptAt: Long,
    val intervalDays: Int
)
