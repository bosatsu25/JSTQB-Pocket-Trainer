package jp.co.testreason.core.domain

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.ReviewSchedule
import jp.co.testreason.core.model.TimeProvider
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Review scheduling rules (docs/ux/LEARNING_LOOP.md, "Review Scheduling").
 *
 * - Incorrect: the interval resets; the mistake reason decides how soon (concept/terminology/untagged
 *   1 day, misread 2 days, careless 3 days). The previous interval is ignored.
 * - Correct with GUESS: treated like a miss, 1 day.
 * - Correct otherwise: the first interval depends on confidence (HIGH 7, MEDIUM 3, LOW/unspecified 1),
 *   later intervals grow from the previous one (HIGH x2.5, MEDIUM x2, LOW/unspecified x1.5),
 *   always by at least one day, capped at [MAX_INTERVAL_DAYS].
 */
class SpacedReviewEngine @Inject constructor(
    private val timeProvider: TimeProvider
) {

    fun calculateSchedule(attempt: Attempt, previous: ReviewSchedule? = null): ReviewSchedule {
        val intervalDays = intervalDays(attempt, previous?.intervalDays)
        return ReviewSchedule(
            questionId = attempt.questionId,
            nextReviewAt = timeProvider.currentTimeMillis() + intervalDays * DAY_MS,
            lastAttemptAt = attempt.timestamp,
            intervalDays = intervalDays
        )
    }

    private fun intervalDays(attempt: Attempt, previousInterval: Int?): Int {
        if (!attempt.isCorrect) return missInterval(attempt.mistakeReason)
        val confidence = attempt.confidence
        if (confidence == ConfidenceLevel.GUESS) return 1
        if (previousInterval == null || previousInterval <= 0) return firstInterval(confidence)
        val grown = ceil(previousInterval * growthFactor(confidence)).toInt()
        return min(MAX_INTERVAL_DAYS, max(previousInterval + 1, grown))
    }

    private fun missInterval(reason: MistakeReason?): Int = when (reason) {
        MistakeReason.CARELESS -> 3
        MistakeReason.MISREAD -> 2
        MistakeReason.CONCEPT, MistakeReason.TERMINOLOGY, null -> 1
    }

    private fun firstInterval(confidence: ConfidenceLevel?): Int = when (confidence) {
        ConfidenceLevel.HIGH -> 7
        ConfidenceLevel.MEDIUM -> 3
        ConfidenceLevel.LOW, ConfidenceLevel.GUESS, null -> 1
    }

    private fun growthFactor(confidence: ConfidenceLevel?): Double = when (confidence) {
        ConfidenceLevel.HIGH -> 2.5
        ConfidenceLevel.MEDIUM -> 2.0
        ConfidenceLevel.LOW, ConfidenceLevel.GUESS, null -> 1.5
    }

    companion object {
        const val MAX_INTERVAL_DAYS = 60
        private const val DAY_MS = 24 * 3600 * 1000L
    }
}
