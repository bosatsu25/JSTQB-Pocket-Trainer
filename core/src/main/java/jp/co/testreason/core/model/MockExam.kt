package jp.co.testreason.core.model

/** A group of learning objectives the exam draws [questionCount] questions from (docs/product/EXAM_FORMAT.md). */
data class ExamGroup(
    val learningObjectiveIds: List<String>,
    val kLevel: KLevel,
    val questionCount: Int
) {
    val chapter: Int get() = learningObjectiveIds.first().removePrefix("FL-").substringBefore('.').toInt()
}

/**
 * CTFL v4.0 exam structure from ISTQB "Exam Structure and Rules Tables" v1.18 (May 27, 2026);
 * JSTQB defers the pass line to ISTQB. See docs/product/EXAM_FORMAT.md for sources.
 */
@Suppress("MagicNumber")
object ExamBlueprint {
    const val SOURCE = "ISTQB Exam Structure and Rules Tables v1.18"
    const val TOTAL_QUESTIONS = 40
    const val PASS_MARK = 26
    const val DURATION_MINUTES = 60
    const val EXTENDED_DURATION_MINUTES = 75

    val groups: List<ExamGroup> = listOf(
        group(KLevel.K1, 1, "1.1.1", "1.2.2"),
        group(KLevel.K1, 1, "1.5.2"),
        group(KLevel.K2, 1, "1.1.2", "1.2.1"),
        group(KLevel.K2, 1, "1.2.3", "1.3.1"),
        group(KLevel.K2, 3, "1.4.1", "1.4.2", "1.4.3", "1.4.4", "1.4.5"),
        group(KLevel.K2, 1, "1.5.1", "1.5.3"),
        group(KLevel.K1, 1, "2.1.2"),
        group(KLevel.K1, 1, "2.1.3"),
        group(KLevel.K2, 1, "2.2.1", "2.2.2"),
        group(KLevel.K2, 1, "2.2.3", "2.3.1"),
        group(KLevel.K2, 1, "2.1.1", "2.1.6"),
        group(KLevel.K2, 1, "2.1.4", "2.1.5"),
        group(KLevel.K1, 2, "3.1.1", "3.2.1", "3.2.3", "3.2.5"),
        group(KLevel.K2, 1, "3.1.2", "3.1.3"),
        group(KLevel.K2, 1, "3.2.2", "3.2.4"),
        group(KLevel.K2, 1, "4.1.1"),
        group(KLevel.K2, 2, "4.3.1", "4.3.2", "4.3.3"),
        group(KLevel.K2, 2, "4.4.1", "4.4.2", "4.4.3"),
        group(KLevel.K2, 1, "4.5.1", "4.5.2"),
        group(KLevel.K3, 5, "4.2.1", "4.2.2", "4.2.3", "4.2.4", "4.5.3"),
        group(KLevel.K1, 1, "5.1.2", "5.1.6", "5.2.1", "5.3.1"),
        group(KLevel.K2, 1, "5.1.1", "5.1.3"),
        group(KLevel.K2, 1, "5.1.7"),
        group(KLevel.K2, 1, "5.2.2", "5.2.3", "5.2.4"),
        group(KLevel.K2, 1, "5.3.2", "5.3.3"),
        group(KLevel.K2, 1, "5.4.1"),
        group(KLevel.K3, 3, "5.1.4", "5.1.5", "5.5.1"),
        group(KLevel.K2, 1, "6.1.1"),
        group(KLevel.K1, 1, "6.2.1")
    )

    fun durationMinutes(extendedTime: Boolean) = if (extendedTime) EXTENDED_DURATION_MINUTES else DURATION_MINUTES

    fun isPass(correct: Int) = correct >= PASS_MARK

    private fun group(kLevel: KLevel, count: Int, vararg sections: String) =
        ExamGroup(sections.map { "FL-$it" }, kLevel, count)
}

data class MockExamQuestion(
    val question: Question,
    val selectedChoiceIds: Set<String>,
    val flagged: Boolean
) {
    val isAnswered: Boolean get() = selectedChoiceIds.size == question.selectCount
}

data class MockExam(
    val sessionId: String,
    val questions: List<MockExamQuestion>,
    val startedAt: Long,
    val durationMinutes: Int,
    val submittedAt: Long?
) {
    val deadline: Long get() = startedAt + durationMinutes * MINUTE_MS
    val isSubmitted: Boolean get() = submittedAt != null
    val answeredCount: Int get() = questions.count { it.isAnswered }

    fun remainingMs(now: Long): Long = (deadline - now).coerceAtLeast(0L)

    private companion object {
        const val MINUTE_MS = 60_000L
    }
}

data class ScoreLine(val label: String, val correct: Int, val total: Int)

data class MockExamResultItem(
    val number: Int,
    val question: Question,
    val selectedChoiceIds: Set<String>,
    val isCorrect: Boolean
)

data class MockExamResult(
    val sessionId: String,
    val correct: Int,
    val total: Int,
    val byChapter: List<ScoreLine>,
    val byKLevel: List<ScoreLine>,
    val items: List<MockExamResultItem>
) {
    val passed: Boolean get() = ExamBlueprint.isPass(correct)
    val unanswered: Int get() = items.count { it.selectedChoiceIds.isEmpty() }
}

sealed interface MockExamStart {
    data class Started(val sessionId: String) : MockExamStart

    /** The question bank cannot fill these blueprint groups. */
    data class NotEnoughQuestions(val missingGroups: List<ExamGroup>) : MockExamStart
}
