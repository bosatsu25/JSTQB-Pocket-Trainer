package jp.co.testreason.feature.analytics

import jp.co.testreason.core.model.Mastery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterProgressTest {

    @Test
    fun noAnswers_listsAllSixChaptersAsNotStarted() {
        val progress = chapterProgress(emptyList())

        assertEquals(listOf(1, 2, 3, 4, 5, 6), progress.map { it.chapter })
        assertEquals(listOf(14, 10, 8, 14, 16, 2), progress.map { it.totalObjectives })
        progress.forEach { assertNull(it.averageMastery) }
    }

    @Test
    fun masteries_areAveragedPerChapter() {
        val progress = chapterProgress(
            listOf(
                Mastery("FL-4.2.1", 1.0f, 0L, 2, 2),
                Mastery("FL-4.2.2", 0.5f, 0L, 2, 1),
                Mastery("FL-1.1.1", 0.0f, 0L, 1, 0)
            )
        )

        val chapter4 = progress.single { it.chapter == 4 }
        assertEquals(2, chapter4.answeredObjectives)
        assertEquals(0.75f, chapter4.averageMastery!!, 0.001f)
        assertEquals(0f, progress.single { it.chapter == 1 }.averageMastery!!, 0.001f)
    }
}
