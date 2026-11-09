package jp.co.testreason.feature.chapter

import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.Choice
import jp.co.testreason.core.model.Mastery
import jp.co.testreason.core.model.Question
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterOverviewTest {

    private fun question(id: String, lo: String) =
        Question(id, lo, "stem", listOf(Choice("A", "a", true, "x")), "exp")

    @Test
    fun overview_countsQuestionsUnseenAndMasteryPerChapter() {
        val questions = listOf(question("c4-001", "FL-4.2.1"), question("c4-002", "FL-4.2.2"), question("c1-001", "FL-1.1.1"))
        val attempts = listOf(Attempt("a", "s", "c4-001", listOf("A"), true, null, null, 0L, 0L))
        val masteries = listOf(Mastery("FL-4.2.1", 0.75f, 0L, 1, 1))

        val overview = chapterOverview(questions, attempts, masteries)

        assertEquals(6, overview.size)
        val chapter4 = overview.single { it.chapter == 4 }
        assertEquals(2, chapter4.questionCount)
        assertEquals(1, chapter4.unseenCount)
        assertEquals(0.75f, chapter4.mastery!!, 0.001f)
        assertNull(overview.single { it.chapter == 1 }.mastery)
        assertEquals(0, overview.single { it.chapter == 6 }.questionCount)
    }
}
