package jp.co.testreason.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the blueprint to ISTQB Exam Structure and Rules Tables v1.18 (docs/product/EXAM_FORMAT.md). */
class ExamBlueprintTest {

    private val groups = ExamBlueprint.groups

    @Test
    fun totalsMatchTheOfficialTables() {
        assertEquals(40, groups.sumOf { it.questionCount })
        assertEquals(ExamBlueprint.TOTAL_QUESTIONS, groups.sumOf { it.questionCount })
        assertEquals(
            mapOf(KLevel.K1 to 8, KLevel.K2 to 24, KLevel.K3 to 8),
            groups.groupBy { it.kLevel }.mapValues { (_, g) -> g.sumOf { it.questionCount } }
        )
        assertEquals(
            mapOf(1 to 8, 2 to 6, 3 to 4, 4 to 11, 5 to 9, 6 to 2),
            groups.groupBy { it.chapter }.mapValues { (_, g) -> g.sumOf { it.questionCount } }
        )
    }

    @Test
    fun everyLearningObjectiveBelongsToExactlyOneGroupOfItsOwnKLevel() {
        val listed = groups.flatMap { it.learningObjectiveIds }
        assertEquals(64, listed.size)
        assertEquals(Syllabus.learningObjectives.map { it.id }.toSet(), listed.toSet())
        groups.forEach { group ->
            group.learningObjectiveIds.forEach { id ->
                assertEquals("$id in $group", group.kLevel, Syllabus.findLearningObjective(id)!!.kLevel)
                assertEquals("$id chapter", group.chapter, Syllabus.findLearningObjective(id)!!.chapter)
            }
        }
    }

    @Test
    fun passMarkIs26of40_andDurationIs60or75Minutes() {
        assertFalse(ExamBlueprint.isPass(25))
        assertTrue(ExamBlueprint.isPass(26))
        assertEquals(60, ExamBlueprint.durationMinutes(extendedTime = false))
        assertEquals(75, ExamBlueprint.durationMinutes(extendedTime = true))
    }
}
