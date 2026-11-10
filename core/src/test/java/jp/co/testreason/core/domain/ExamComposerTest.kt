package jp.co.testreason.core.domain

import jp.co.testreason.core.content.QuestionContentParser
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.ExamBlueprint
import jp.co.testreason.core.model.Question
import jp.co.testreason.core.model.Syllabus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class ExamComposerTest {

    @Test
    fun composition_followsTheBlueprintGroupByGroup() {
        val bank = ExamTestBank.questions(perLearningObjective = 2)
        val exam = (ExamComposer.compose(bank, Random(7)) as ExamComposition.Ready).questions

        assertEquals(40, exam.size)
        assertEquals(40, exam.map { it.id }.toSet().size)
        ExamBlueprint.groups.forEach { group ->
            assertEquals(group.toString(), group.questionCount, exam.count { it.learningObjectiveId in group.learningObjectiveIds })
        }
        // Ordered by blueprint group, i.e. by chapter.
        assertEquals(exam.map { it.learningObjective!!.chapter }.sorted(), exam.map { it.learningObjective!!.chapter })
    }

    @Test
    fun largeGroups_spreadOverDifferentObjectives() {
        val bank = ExamTestBank.questions(perLearningObjective = 3)
        val exam = (ExamComposer.compose(bank, Random(1)) as ExamComposition.Ready).questions
        val k3Chapter4 = exam.filter { it.learningObjectiveId in setOf("FL-4.2.1", "FL-4.2.2", "FL-4.2.3", "FL-4.2.4", "FL-4.5.3") }
        assertEquals(5, k3Chapter4.map { it.learningObjectiveId }.toSet().size)
    }

    @Test
    fun missingQuestions_areReportedPerGroup() {
        val bank = ExamTestBank.questions(perLearningObjective = 2).filterNot { it.learningObjectiveId == "FL-6.2.1" }
        val result = ExamComposer.compose(bank) as ExamComposition.Incomplete
        assertEquals(listOf(listOf("FL-6.2.1")), result.missingGroups.map { it.learningObjectiveIds })
    }

    @Test
    fun bundledBank_supportsThreeExamsWithoutRepeatingAQuestion() {
        val bank = File("src/main/assets/questions").listFiles { f -> f.extension == "json" }!!
            .flatMap { file -> QuestionContentParser.parse(file.readText()).questions.filter { it.active }.map { it.question } }
        val byLo = bank.groupingBy { it.learningObjectiveId }.eachCount()
        val short = ExamBlueprint.groups.filter { group ->
            group.learningObjectiveIds.sumOf { byLo[it] ?: 0 } < group.questionCount * DISJOINT_EXAMS
        }
        assertTrue("Groups without enough questions for $DISJOINT_EXAMS exams: $short", short.isEmpty())
    }

    private companion object {
        const val DISJOINT_EXAMS = 3
    }
}

/** Synthetic bank: [perLearningObjective] questions for every LO, correct answer always "A". */
object ExamTestBank {
    fun questions(perLearningObjective: Int): List<Question> = Syllabus.learningObjectives.flatMap { lo ->
        (1..perLearningObjective).map { n ->
            Question(
                id = "${lo.id}-$n",
                learningObjectiveId = lo.id,
                stem = "${lo.id} 問題$n",
                choices = listOf("A", "B", "C", "D").map { Choice(it, "選択肢$it", it == "A", "解説") },
                explanation = "解説",
                revision = 1
            )
        }
    }
}
