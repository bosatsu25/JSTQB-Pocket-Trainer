package jp.co.testreason.feature.drill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrillLapTest {

    @Test
    fun correctAnswers_clearTheQueueInOrder() {
        var lap = DrillLap.start(listOf("a", "b", "c"))
        assertEquals("a", lap.current)

        lap = lap.answered(correct = true).answered(correct = true)
        assertEquals("c", lap.current)
        assertEquals(2, lap.clearedCount)
        assertFalse(lap.isFinished)

        lap = lap.answered(correct = true)
        assertTrue(lap.isFinished)
        assertNull(lap.current)
        assertEquals(3, lap.firstTryCorrectCount)
        assertEquals(3, lap.answerCount)
    }

    @Test
    fun wrongAnswer_comesBackAtTheEnd_untilAnsweredCorrectly() {
        var lap = DrillLap.start(listOf("a", "b", "c"))

        lap = lap.answered(correct = false) // a missed
        assertEquals(listOf("b", "c", "a"), lap.queue)
        lap = lap.answered(correct = true).answered(correct = true) // b, c
        assertEquals("a", lap.current)
        lap = lap.answered(correct = false) // a missed again: still asked
        assertEquals(listOf("a"), lap.queue)
        assertFalse(lap.isFinished)
        lap = lap.answered(correct = true)

        assertTrue(lap.isFinished)
        assertEquals(setOf("a"), lap.missed)
        assertEquals(2, lap.firstTryCorrectCount)
        assertEquals(5, lap.answerCount)
    }

    @Test
    fun firstTryCorrectCount_ignoresQuestionsNotYetShown() {
        val lap = DrillLap.start(listOf("a", "b", "c")).answered(correct = true)
        assertEquals(1, lap.firstTryCorrectCount)
    }

    @Test
    fun emptyOrDuplicateInput_isHandled() {
        assertTrue(DrillLap.start(emptyList()).isFinished)
        assertEquals(2, DrillLap.start(listOf("a", "a", "b")).total)
        assertEquals(DrillLap.start(emptyList()), DrillLap.start(emptyList()).answered(true))
    }
}
