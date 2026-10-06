package jp.co.testreason.core.domain

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import javax.inject.Inject

data class ReviewPriority(
    val questionId: String,
    val priorityScore: Float,
    val recommendedNextReviewTimestamp: Long
)

class SpacedReviewEngine @Inject constructor() {

    fun calculatePriority(attempt: Attempt): ReviewPriority {
        var baseScore = if (attempt.isCorrect) 10f else 50f

        when (attempt.confidence) {
            ConfidenceLevel.GUESS -> baseScore += 30f
            ConfidenceLevel.LOW -> baseScore += 20f
            ConfidenceLevel.MEDIUM -> baseScore += 10f
            ConfidenceLevel.HIGH -> baseScore += 0f
            null -> baseScore += 15f
        }

        when (attempt.mistakeReason) {
            MistakeReason.CONCEPT -> baseScore += 25f
            MistakeReason.TERMINOLOGY -> baseScore += 20f
            MistakeReason.MISREAD -> baseScore += 10f
            MistakeReason.CARELESS -> baseScore += 5f
            null -> { }
        }

        // Interval calculation (ms)
        val intervalDays = when {
            baseScore >= 60f -> 1L
            baseScore >= 30f -> 3L
            else -> 7L
        }
        val nextReviewMs = attempt.timestamp + (intervalDays * 24 * 3600 * 1000L)

        return ReviewPriority(
            questionId = attempt.questionId,
            priorityScore = baseScore,
            recommendedNextReviewTimestamp = nextReviewMs
        )
    }
}
