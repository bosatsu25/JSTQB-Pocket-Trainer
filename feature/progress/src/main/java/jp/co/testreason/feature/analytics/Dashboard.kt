package jp.co.testreason.feature.analytics

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.KLevel
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.Syllabus

/** Learner-facing state of one LO (docs/ux/LEARNING_LOOP.md, "Learning Objective States"). */
enum class ObjectiveState(val label: String) {
    NOT_STARTED("未着手"),
    WEAK("弱点"),
    LEARNING("学習中"),
    MASTERED("習熟")
}

data class ObjectiveStatus(
    val id: String,
    val kLevel: KLevel,
    val summary: String,
    val state: ObjectiveState,
    val score: Float?,
    val questionCount: Int
)

data class KLevelAccuracy(val kLevel: KLevel, val answered: Int, val correct: Int) {
    val percent: Int? get() = if (answered == 0) null else correct * PERCENT / answered

    private companion object {
        const val PERCENT = 100
    }
}

data class MistakeCount(val reason: MistakeReason?, val count: Int)

const val WEAK_BELOW = 0.6f
const val MASTERED_FROM = 0.8f
const val MASTERED_MIN_ANSWERS = 2

fun objectiveState(mastery: Mastery?): ObjectiveState = when {
    mastery == null || mastery.totalAttempts == 0 -> ObjectiveState.NOT_STARTED
    mastery.score < WEAK_BELOW -> ObjectiveState.WEAK
    mastery.score >= MASTERED_FROM && mastery.totalAttempts >= MASTERED_MIN_ANSWERS -> ObjectiveState.MASTERED
    else -> ObjectiveState.LEARNING
}

/** Builds the whole dashboard from raw data; pure so it can be unit-tested. */
fun buildDashboard(questions: List<Question>, attempts: List<Attempt>, masteries: List<Mastery>): AnalyticsUiState {
    val masteryByLo = masteries.associateBy { it.learningObjectiveId }
    val questionCountByLo = questions.groupingBy { it.learningObjectiveId }.eachCount()
    val loByQuestion = questions.associate { it.id to it.learningObjectiveId }

    val objectives = Syllabus.learningObjectives.map { lo ->
        val mastery = masteryByLo[lo.id]
        ObjectiveStatus(
            id = lo.id,
            kLevel = lo.kLevel,
            summary = lo.summary,
            state = objectiveState(mastery),
            score = mastery?.takeIf { it.totalAttempts > 0 }?.score,
            questionCount = questionCountByLo[lo.id] ?: 0
        )
    }

    val kLevels = KLevel.entries.map { level ->
        val answers = attempts.filter { attempt ->
            loByQuestion[attempt.questionId]?.let { Syllabus.findLearningObjective(it)?.kLevel } == level
        }
        KLevelAccuracy(level, answers.size, answers.count { it.isCorrect })
    }

    val misses = attempts.filter { !it.isCorrect }
    val mistakes = (MistakeReason.entries.map { reason -> MistakeCount(reason, misses.count { it.mistakeReason == reason }) } +
        MistakeCount(null, misses.count { it.mistakeReason == null })).filter { it.count > 0 }

    return AnalyticsUiState(
        chapters = chapterProgress(masteries).map { chapter ->
            chapter.copy(objectives = objectives.filter { Syllabus.findLearningObjective(it.id)?.chapter == chapter.chapter })
        },
        kLevels = kLevels,
        mistakes = mistakes
    )
}
