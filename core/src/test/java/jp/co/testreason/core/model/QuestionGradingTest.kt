package jp.co.testreason.core.model

import jp.co.testreason.core.data.toDomain
import jp.co.testreason.core.data.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionGradingTest {

    private val choices = listOf("A", "B", "C", "D", "E").map { Choice(it, "text $it", it in setOf("B", "D"), "x") }
    private val multi = Question("q", "FL-1.1.1", "stem", choices, "exp", selectCount = 2)

    @Test
    fun multiAnswer_onlyExactSetIsCorrect() {
        assertTrue(multi.isCorrectSelection(listOf("D", "B")))
        assertFalse(multi.isCorrectSelection(listOf("B")))
        assertFalse(multi.isCorrectSelection(listOf("B", "C")))
        assertFalse(multi.isCorrectSelection(listOf("B", "D", "E")))
    }

    @Test
    fun attemptSelection_roundTripsThroughEntity() {
        val attempt = Attempt("a", "s", "q", listOf("D", "B"), true, null, null, 0L, 0L)

        val entity = attempt.toEntity()

        assertEquals("B,D", entity.selectedChoiceId)
        assertEquals(listOf("B", "D"), entity.toDomain().selectedChoiceIds)
    }
}
