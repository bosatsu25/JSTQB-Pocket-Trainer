package jp.co.testreason.feature.result

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.Syllabus
import jp.co.testreason.core.ui.components.label

/** One row of the confidence x correctness breakdown; `confidence == null` means "not selected". */
data class ConfidenceRow(val confidence: ConfidenceLevel?, val correct: Int, val incorrect: Int) {
    val label: String get() = confidence?.label ?: "未選択"
}

data class ObjectiveResult(val id: String, val summary: String)

data class SessionSummary(
    val totalTimeMs: Long = 0L,
    val confidenceRows: List<ConfidenceRow> = emptyList(),
    /** LOs answered correctly and confidently in this session. */
    val strongObjectives: List<ObjectiveResult> = emptyList(),
    /** LOs with a wrong, guessed or low-confidence answer in this session. */
    val reviewObjectives: List<ObjectiveResult> = emptyList()
) {
    /** "3分05秒" style duration. */
    val totalTimeText: String
        get() {
            val seconds = totalTimeMs / MILLIS_PER_SECOND
            return "${seconds / SECONDS_PER_MINUTE}分${(seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')}秒"
        }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val SECONDS_PER_MINUTE = 60L
    }
}

/** Builds the result breakdown; [objectiveOf] maps a question id to its LO id. */
fun summarizeSession(attempts: List<Attempt>, objectiveOf: (String) -> String?): SessionSummary {
    val rows = (listOf<ConfidenceLevel?>(null) + ConfidenceLevel.entries).mapNotNull { confidence ->
        val group = attempts.filter { it.confidence == confidence }
        if (group.isEmpty()) null else ConfidenceRow(confidence, group.count { it.isCorrect }, group.count { !it.isCorrect })
    }

    val byObjective = attempts.groupBy { objectiveOf(it.questionId) }.filterKeys { it != null }
    val needsReview = byObjective.filterValues { answers -> answers.any { it.needsReview() } }.keys.filterNotNull()
    val strong = byObjective.keys.filterNotNull() - needsReview.toSet()

    return SessionSummary(
        totalTimeMs = attempts.sumOf { it.timeSpentMs },
        confidenceRows = rows,
        strongObjectives = strong.sorted().map { it.toObjectiveResult() },
        reviewObjectives = needsReview.sorted().map { it.toObjectiveResult() }
    )
}

private fun Attempt.needsReview(): Boolean =
    !isCorrect || confidence == ConfidenceLevel.GUESS || confidence == ConfidenceLevel.LOW

private fun String.toObjectiveResult() = ObjectiveResult(this, Syllabus.findLearningObjective(this)?.summary.orEmpty())
