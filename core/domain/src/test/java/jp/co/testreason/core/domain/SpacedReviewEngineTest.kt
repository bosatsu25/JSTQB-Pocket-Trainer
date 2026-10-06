package jp.co.testreason.core.domain

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.TimeProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class FixedTimeProvider(val fixedTime: Long) : TimeProvider {
    override fun currentTimeMillis(): Long = fixedTime
}

class SpacedReviewEngineTest {

    private val timeProvider = FixedTimeProvider(1000000L)
    private val engine = SpacedReviewEngine(timeProvider)

    @Test
    fun calculateSchedule_incorrectWithConceptMistake_schedules1DayInterval() {
        val attempt = Attempt(
            id = "a1",
            sessionId = "s1",
            questionId = "q1",
            selectedChoiceId = "A",
            isCorrect = false,
            confidence = ConfidenceLevel.GUESS,
            mistakeReason = MistakeReason.CONCEPT,
            timeSpentMs = 2000,
            timestamp = 1000000L
        )

        val schedule = engine.calculateSchedule(attempt)
        assertEquals(1, schedule.intervalDays)
        assertEquals(1000000L + 1 * 86400000L, schedule.nextReviewAt)
        assertEquals("q1", schedule.questionId)
    }

    @Test
    fun calculateSchedule_correctWithHighConfidence_schedules7DayInterval() {
        val attempt = Attempt(
            id = "a2",
            sessionId = "s1",
            questionId = "q2",
            selectedChoiceId = "B",
            isCorrect = true,
            confidence = ConfidenceLevel.HIGH,
            mistakeReason = null,
            timeSpentMs = 1500,
            timestamp = 1000000L
        )

        val schedule = engine.calculateSchedule(attempt)
        assertEquals(7, schedule.intervalDays)
        assertEquals(1000000L + 7 * 86400000L, schedule.nextReviewAt)
    }
}
