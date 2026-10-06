package jp.co.testreason.core.domain

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.ReviewSchedule
import jp.co.testreason.core.model.TimeProvider
import javax.inject.Inject

class SpacedReviewEngine @Inject constructor(
    private val timeProvider: TimeProvider
) {

    fun calculateSchedule(attempt: Attempt): ReviewSchedule {
        val accuracyScore = if (attempt.isCorrect) 10f else 50f
        val totalScore = accuracyScore + confidenceScore(attempt.confidence) + mistakeScore(attempt.mistakeReason)

        val intervalDays = when {
            totalScore >= 60f -> 1
            totalScore >= 30f -> 3
            else -> 7
        }

        val now = timeProvider.currentTimeMillis()
        val nextReviewMs = now + (intervalDays * 24 * 3600 * 1000L)

        return ReviewSchedule(
            questionId = attempt.questionId,
            nextReviewAt = nextReviewMs,
            lastAttemptAt = attempt.timestamp,
            intervalDays = intervalDays
        )
    }

    private fun confidenceScore(confidence: ConfidenceLevel?): Float = when (confidence) {
        ConfidenceLevel.GUESS -> 30f
        ConfidenceLevel.LOW -> 20f
        ConfidenceLevel.MEDIUM -> 10f
        ConfidenceLevel.HIGH, null -> 0f
    }

    private fun mistakeScore(mistakeReason: MistakeReason?): Float = when (mistakeReason) {
        MistakeReason.CONCEPT -> 25f
        MistakeReason.TERMINOLOGY -> 20f
        MistakeReason.MISREAD -> 10f
        MistakeReason.CARELESS -> 5f
        null -> 0f
    }
}
