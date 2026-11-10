package jp.co.testreason.core.domain

import jp.co.testreason.core.model.ExamBlueprint
import jp.co.testreason.core.model.ExamGroup
import jp.co.testreason.core.model.Question
import kotlin.random.Random

sealed interface ExamComposition {
    data class Ready(val questions: List<Question>) : ExamComposition
    data class Incomplete(val missingGroups: List<ExamGroup>) : ExamComposition
}

/**
 * Picks questions per [ExamBlueprint] group. Within a group the LOs are visited round-robin in random order,
 * so a 3-question group spreads over three LOs when it can. Output is ordered by blueprint group (= chapter).
 */
object ExamComposer {

    fun compose(questions: List<Question>, random: Random = Random.Default): ExamComposition {
        val byLo = questions.groupBy { it.learningObjectiveId }
        val missing = ExamBlueprint.groups.filter { group ->
            group.learningObjectiveIds.sumOf { byLo[it]?.size ?: 0 } < group.questionCount
        }
        if (missing.isNotEmpty()) return ExamComposition.Incomplete(missing)

        val picked = ExamBlueprint.groups.flatMap { group -> pick(group, byLo, random) }
        return ExamComposition.Ready(picked)
    }

    private fun pick(group: ExamGroup, byLo: Map<String, List<Question>>, random: Random): List<Question> {
        val pools = group.learningObjectiveIds.shuffled(random)
            .map { lo -> byLo[lo].orEmpty().shuffled(random).toMutableList() }
            .filter { it.isNotEmpty() }
        val result = mutableListOf<Question>()
        while (result.size < group.questionCount) {
            pools.filter { it.isNotEmpty() }.forEach { pool ->
                if (result.size < group.questionCount) result += pool.removeAt(0)
            }
        }
        return result
    }
}
