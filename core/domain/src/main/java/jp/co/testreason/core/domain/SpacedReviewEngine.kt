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
        var baseScore = if (attempt.isCorrect) 10f else 50f

        when (attempt.confidence) {
            ConfidenceLevel.GUESS -> baseScore += 30f
            ConfidenceLevel.LOW -> baseScore += 20f
            ConfidenceLevel.MEDIUM -> baseScore += 10f
            ConfidenceLevel.HIGH -> baseScore += 0f
            null -> { /* Unspecified / No observation */ }
        }

        when (attempt.mistakeReason) {
            MistakeReason.CONCEPT -> baseScore += 25f
            MistakeReason.TERMINOLOGY -> baseScore += 20f
            MistakeReason.MISREAD -> baseScore += 10f
            MistakeReason.CARELESS -> baseScore += 5f
            null -> { }
        }

        val intervalDays = when {
            baseScore >= 60f -> 1
            baseScore >= 30f -> 3
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
}
