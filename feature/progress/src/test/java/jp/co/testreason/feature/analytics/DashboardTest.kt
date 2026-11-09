package jp.co.testreason.feature.analytics

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.KLevel
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DashboardTest {

    private fun question(id: String, lo: String) = Question(id, lo, "stem", listOf(Choice("A", "a", true, "x")), "exp")
    private fun attempt(id: String, q: String, correct: Boolean, reason: MistakeReason? = null) =
        Attempt(id, "s_$id", q, listOf("A"), correct, null, reason, 0L, 0L)

    @Test
    fun objectiveState_thresholds() {
        assertEquals(ObjectiveState.NOT_STARTED, objectiveState(null))
        assertEquals(ObjectiveState.WEAK, objectiveState(Mastery("x", 0.59f, 0L, 5, 3)))
        assertEquals(ObjectiveState.LEARNING, objectiveState(Mastery("x", 0.6f, 0L, 5, 3)))
        // One perfect answer is not enough to call an LO mastered.
        assertEquals(ObjectiveState.LEARNING, objectiveState(Mastery("x", 1f, 0L, 1, 1)))
        assertEquals(ObjectiveState.MASTERED, objectiveState(Mastery("x", 0.8f, 0L, 2, 2)))
    }

    @Test
    fun dashboard_aggregatesKLevelsMistakesAndObjectives() {
        val questions = listOf(question("c1-001", "FL-1.1.1"), question("c4-001", "FL-4.2.1"), question("c4-002", "FL-4.2.1"))
        val attempts = listOf(
            attempt("a1", "c1-001", correct = true),
            attempt("a2", "c4-001", correct = false, reason = MistakeReason.CONCEPT),
            attempt("a3", "c4-002", correct = true),
            attempt("a4", "c4-001", correct = false)
        )
        val dashboard = buildDashboard(questions, attempts, listOf(Mastery("FL-4.2.1", 0.33f, 0L, 3, 1)))

        assertEquals(KLevelAccuracy(KLevel.K1, 1, 1), dashboard.kLevels.single { it.kLevel == KLevel.K1 })
        assertEquals(0, dashboard.kLevels.single { it.kLevel == KLevel.K2 }.answered)
        assertNull(dashboard.kLevels.single { it.kLevel == KLevel.K2 }.percent)
        assertEquals(33, dashboard.kLevels.single { it.kLevel == KLevel.K3 }.percent)
        assertEquals(listOf(MistakeCount(MistakeReason.CONCEPT, 1), MistakeCount(null, 1)), dashboard.mistakes)

        assertEquals(64, dashboard.chapters.sumOf { it.objectives.size })
        val lo = dashboard.chapters.single { it.chapter == 4 }.objectives.single { it.id == "FL-4.2.1" }
        assertEquals(ObjectiveState.WEAK, lo.state)
        assertEquals(2, lo.questionCount)
    }
}
