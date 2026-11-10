package jp.co.testreason.feature.result

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSummaryTest {

    private fun attempt(q: String, correct: Boolean, confidence: ConfidenceLevel?, ms: Long = 30_000L) =
        Attempt("a_$q", "s", q, listOf("A"), correct, confidence, null, ms, 0L)

    private val objectives = mapOf("q1" to "FL-1.1.1", "q2" to "FL-1.1.1", "q3" to "FL-4.2.2", "q4" to "FL-5.1.4")

    @Test
    fun summary_groupsByConfidenceAndSplitsObjectives() {
        val summary = summarizeSession(
            listOf(
                attempt("q1", true, ConfidenceLevel.HIGH),
                attempt("q2", true, ConfidenceLevel.GUESS),
                attempt("q3", true, null, ms = 65_000L),
                attempt("q4", false, ConfidenceLevel.HIGH)
            )
        ) { objectives[it] }

        assertEquals(155_000L, summary.totalTimeMs)
        assertEquals("2分35秒", summary.totalTimeText)
        assertEquals(
            listOf(
                ConfidenceRow(null, 1, 0),
                ConfidenceRow(ConfidenceLevel.GUESS, 1, 0),
                ConfidenceRow(ConfidenceLevel.HIGH, 1, 1)
            ),
            summary.confidenceRows
        )
        // FL-1.1.1 had a guessed answer, FL-5.1.4 a wrong one; only FL-4.2.2 counts as strong.
        assertEquals(listOf("FL-4.2.2"), summary.strongObjectives.map { it.id })
        assertEquals(listOf("FL-1.1.1", "FL-5.1.4"), summary.reviewObjectives.map { it.id })
        assertEquals("境界値分析の適用", summary.strongObjectives.single().summary)
    }

    @Test
    fun emptySession_hasNoRows() {
        val summary = summarizeSession(emptyList()) { null }
        assertEquals("0分00秒", summary.totalTimeText)
        assertEquals(emptyList<ConfidenceRow>(), summary.confidenceRows)
    }
}
