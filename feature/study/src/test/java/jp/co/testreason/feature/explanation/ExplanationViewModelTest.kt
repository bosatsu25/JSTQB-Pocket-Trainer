package jp.co.testreason.feature.explanation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import jp.co.testreason.core.model.Attempt
import jp.co.testreason.core.model.ConfidenceLevel
import jp.co.testreason.core.model.MistakeReason
import jp.co.testreason.feature.testing.MainDispatcherRule
import jp.co.testreason.feature.testing.REPOSITORY_IO_TIMEOUT
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
class ExplanationViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() = runTest {
        env = TestEnvironment()
        env.start()
        env.session("s1", SampleData.single1.id, SampleData.single2.id)
    }

    @After
    fun tearDown() = env.close()

    private suspend fun answerAndOpen(questionId: String, correct: Boolean): ExplanationViewModel {
        val choice = if (correct) "B" else "A"
        env.quizRepository.finalizeAttempt(
            Attempt("att_$questionId", "s1", questionId, listOf(choice), correct, ConfidenceLevel.HIGH, null, 1_000L, env.clock.now)
        )
        val vm = ExplanationViewModel(
            env.quizRepository,
            SavedStateHandle(mapOf("sessionId" to "s1", "questionId" to questionId, "attemptId" to "att_$questionId"))
        )
        vm.uiState.first { it.attempt != null }
        return vm
    }

    @Test
    fun mistakeReason_updatesTheAttemptAndTheReviewInterval() = runTest {
        val vm = answerAndOpen(SampleData.single1.id, correct = false)
        assertFalse(vm.uiState.value.isLastQuestion)

        vm.selectMistakeReason(MistakeReason.CARELESS)

        vm.uiState.first { it.attempt?.mistakeReason == MistakeReason.CARELESS }
        assertEquals(3, env.db.reviewScheduleDao().getSchedule(SampleData.single1.id)!!.intervalDays)
    }

    @Test
    fun proceed_goesToNextQuestionThenToResult() = runTest {
        val first = answerAndOpen(SampleData.single1.id, correct = true)
        first.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            first.proceedToNext()
            assertEquals(ExplanationUiEvent.NavigateToNextQuestion("s1"), awaitItem())
        }

        val last = answerAndOpen(SampleData.single2.id, correct = true)
        assertTrue(last.uiState.value.isLastQuestion)
        last.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            last.proceedToNext()
            assertEquals(ExplanationUiEvent.NavigateToResult("s1"), awaitItem())
        }
        assertTrue(env.quizRepository.getSessionById("s1")!!.isFinalized)
    }

    @Test
    fun secondProceedAfterTheFirstCompleted_isIgnored() = runTest {
        val vm = answerAndOpen(SampleData.single1.id, correct = true)
        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.proceedToNext()
            awaitItem()
            vm.uiState.first { !it.isProcessing }
            vm.proceedToNext()
            vm.uiState.first { !it.isProcessing } // let a (wrongly) started second call finish first
            expectNoEvents()
        }
        assertEquals(1, env.quizRepository.getSessionById("s1")!!.currentQuestionIndex)
    }
}

