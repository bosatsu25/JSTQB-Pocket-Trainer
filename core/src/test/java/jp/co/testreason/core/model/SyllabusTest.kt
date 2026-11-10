package jp.co.testreason.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SyllabusTest {

    @Test
    fun learningObjectives_matchSyllabusCounts() {
        val los = Syllabus.learningObjectives
        assertEquals(64, los.size)
        assertEquals(64, los.map { it.id }.toSet().size)
        assertEquals(mapOf(1 to 14, 2 to 10, 3 to 8, 4 to 14, 5 to 16, 6 to 2), los.groupingBy { it.chapter }.eachCount())
    }

    @Test
    fun k3LearningObjectives_areTheEightApplicationObjectives() {
        val k3 = Syllabus.learningObjectives.filter { it.kLevel == KLevel.K3 }.map { it.id }
        assertEquals(
            listOf("FL-4.2.1", "FL-4.2.2", "FL-4.2.3", "FL-4.2.4", "FL-4.5.3", "FL-5.1.4", "FL-5.1.5", "FL-5.5.1"),
            k3
        )
    }

    @Test
    fun chapters_studyMinutesSumTo1135() {
        assertEquals(6, Syllabus.chapters.size)
        assertEquals(1135, Syllabus.chapters.sumOf { it.studyMinutes })
    }

    @Test
    fun findLearningObjective_resolvesSectionAndChapter() {
        val lo = Syllabus.findLearningObjective("FL-4.2.2")
        assertNotNull(lo)
        assertEquals("4.2.2", lo!!.section)
        assertEquals(4, lo.chapter)
        assertNull(Syllabus.findLearningObjective("FL-9.9.9"))
    }
}
