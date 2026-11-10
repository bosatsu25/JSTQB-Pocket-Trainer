package jp.co.testreason.feature.quiz

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import jp.co.testreason.feature.testing.MainDispatcherRule
import jp.co.testreason.feature.testing.SampleData
import jp.co.testreason.feature.testing.TestEnvironment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuizViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() = runTest {
        env = TestEnvironment()
        env.start()
    }

    @After
    fun tearDown() = env.close()

    private suspend fun viewModelFor(vararg questionIds: String): QuizViewModel {
        val sessionId = env.session("s1", *questionIds)
        val vm = QuizViewModel(env.quizRepository, env.clock, SavedStateHandle(mapOf("sessionId" to sessionId)))
        vm.uiState.first { it.currentQuestion != null }
        return vm
    }

    @Test
    fun singleAnswer_replacesSelection_andSubmitRecordsTimeAndCorrectness() = runTest {
        val vm = viewModelFor(SampleData.single1.id)
        assertFalse(vm.uiState.value.canSubmit)

        vm.selectChoice("A")
        vm.selectChoice("B")
        assertEquals(setOf("B"), vm.uiState.value.selectedChoiceIds)
        assertTrue(vm.uiState.value.canSubmit)

        env.clock.now += 42_000L
        vm.events.test {
            vm.submitAnswer()
            val event = awaitItem() as QuizUiEvent.NavigateToExplanation
            assertEquals(SampleData.single1.id, event.questionId)
        }
        val attempt = env.db.attemptDao().getAttempt("s1", SampleData.single1.id)!!
        assertTrue(attempt.isCorrect)
        assertEquals(42_000L, attempt.timeSpentMs)
    }

    @Test
    fun multiAnswer_togglesUpToTheRequiredCount_andGradesTheExactSet() = runTest {
        val vm = viewModelFor(SampleData.multi.id)

        vm.selectChoice("B")
        assertFalse(vm.uiState.value.canSubmit)
        vm.selectChoice("C")
        vm.selectChoice("D") // ignored: two are already selected
        assertEquals(setOf("B", "C"), vm.uiState.value.selectedChoiceIds)
        vm.selectChoice("C") // toggle off
        vm.selectChoice("D")
        assertEquals(setOf("B", "D"), vm.uiState.value.selectedChoiceIds)

        vm.events.test {
            vm.submitAnswer()
            awaitItem()
        }
        val attempt = env.db.attemptDao().getAttempt("s1", SampleData.multi.id)!!
        assertEquals("B,D", attempt.selectedChoiceId)
        assertTrue(attempt.isCorrect)
    }

    @Test
    fun doubleSubmit_recordsOneAttemptAndNavigatesOnce() = runTest {
        val vm = viewModelFor(SampleData.single1.id)
        vm.selectChoice("A")

        vm.events.test {
            vm.submitAnswer()
            vm.submitAnswer()
            awaitItem()
            expectNoEvents()
        }
        assertFalse(env.db.attemptDao().getAttempt("s1", SampleData.single1.id)!!.isCorrect)
    }
}
