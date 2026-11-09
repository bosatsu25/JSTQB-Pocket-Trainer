package jp.co.testreason.feature.home

import jp.co.testreason.core.model.QuizSession
import jp.co.testreason.core.model.StudyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeUiStateTest {

    private fun session(index: Int, size: Int, finalized: Boolean = false) =
        QuizSession("s", StudyMode.DAILY, List(size) { "q$it" }, index, 0L, null, finalized)

    @Test
    fun sessionWithRemainingQuestions_isResumable() {
        val s = session(index = 2, size = 5)
        assertEquals(s, HomeUiState(activeSession = s).resumableSession)
    }

    @Test
    fun emptyOrFinishedSessions_areNotResumable() {
        assertNull(HomeUiState(activeSession = session(index = 0, size = 0)).resumableSession)
        assertNull(HomeUiState(activeSession = session(index = 5, size = 5)).resumableSession)
        assertNull(HomeUiState(activeSession = session(index = 1, size = 5, finalized = true)).resumableSession)
    }
}
