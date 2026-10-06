package jp.co.testreason.core.domain

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedReviewEngineTest {

    private val engine = SpacedReviewEngine()

    @Test
    fun calculatePriority_incorrectWithConceptMistake_givesHighestPriority() {
        val attempt = Attempt(
            id = "a1",
            sessionId = "s1",
            questionId = "q1",
            selectedChoiceId = "A",
            isCorrect = false,
            confidence = ConfidenceLevel.GUESS,
            mistakeReason = MistakeReason.CONCEPT,
            timeSpentMs = 2000,
            timestamp = 100000
        )

        val priority = engine.calculatePriority(attempt)
        assertTrue(priority.priorityScore >= 80f)
        assertEquals("q1", priority.questionId)
    }
}
