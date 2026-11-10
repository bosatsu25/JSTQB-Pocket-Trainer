package jp.co.testreason.feature.mockexam

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import jp.co.testreason.core.model.MockExamStart
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
class MockExamViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private lateinit var env: TestEnvironment

    @Before
    fun setUp() = runTest {
        env = TestEnvironment(SampleData.examBank())
        env.start()
    }

    @After
    fun tearDown() = env.close()

    private suspend fun startExam(): String = (env.mockExamRepository.startMockExam(false) as MockExamStart.Started).sessionId

    private suspend fun examViewModel(sessionId: String): MockExamViewModel {
        val vm = MockExamViewModel(env.mockExamRepository, env.clock, SavedStateHandle(mapOf("sessionId" to sessionId)))
        vm.uiState.first { it.exam != null }
        return vm
    }

    @Test
    fun formatRemaining_roundsUpToWholeSeconds() {
        assertEquals("60:00", formatRemaining(60 * 60_000L))
        assertEquals("0:01", formatRemaining(1L))
        assertEquals("0:00", formatRemaining(0L))
        assertEquals("4:59", formatRemaining(299_000L))
    }

    @Test
    fun intro_startsAnExam_andOffersToResumeIt() = runTest {
        val vm = MockExamIntroViewModel(env.mockExamRepository)
        vm.setExtendedTime(true)
        lateinit var id: String
        vm.events.test {
            vm.start()
            id = (awaitItem() as MockExamIntroEvent.NavigateToExam).sessionId
        }
        assertEquals(75, env.mockExamRepository.observeExam(id).first()!!.durationMinutes)
        vm.uiState.first { it.resumableExamId == id }
    }

    @Test
    fun answeringFlaggingAndPaging_updateTheNavigatorState() = runTest {
        val vm = examViewModel(startExam())
        assertEquals(40, vm.uiState.value.total)

        vm.select("B")
        vm.select("A") // answers can change before submission
        vm.toggleFlag()
        vm.uiState.first { it.current?.selectedChoiceIds == setOf("A") && it.current?.flagged == true }
        assertEquals(39, vm.uiState.value.unansweredCount)
        assertEquals(1, vm.uiState.value.flaggedCount)

        vm.next()
        assertEquals(1, vm.uiState.value.currentIndex)
        vm.goTo(99)
        assertEquals(39, vm.uiState.value.currentIndex)
        vm.previous()
        assertEquals(38, vm.uiState.value.currentIndex)
    }

    @Test
    fun timeUp_submitsAutomatically_andNavigatesToTheResult() = runTest {
        val id = startExam()
        val vm = examViewModel(id)
        vm.select("A")
        vm.uiState.first { it.unansweredCount == 39 }

        vm.events.test {
            env.clock.now += 60 * 60_000L
            vm.onTick()
            assertEquals(MockExamEvent.NavigateToResult(id), awaitItem())
        }
        val result = env.mockExamRepository.getResult(id)!!
        assertEquals(1, result.correct)
        assertEquals(39, result.unanswered)
        assertFalse(result.passed)
    }

    @Test
    fun reopeningAfterTheDeadline_submitsImmediately() = runTest {
        val id = startExam()
        env.clock.now += 61 * 60_000L // e.g. the app was killed during the exam

        val vm = MockExamViewModel(env.mockExamRepository, env.clock, SavedStateHandle(mapOf("sessionId" to id)))
        vm.events.test {
            assertEquals(MockExamEvent.NavigateToResult(id), awaitItem())
        }
        assertTrue(env.mockExamRepository.observeExam(id).first()!!.isSubmitted)
    }

    @Test
    fun result_showsTheScore_andWrongAnswersFeedTheWeaknessSession() = runTest {
        val id = startExam()
        val questions = env.mockExamRepository.observeExam(id).first()!!.questions
        questions.take(26).forEach { env.mockExamRepository.answer(id, it.question.id, setOf("A")) }
        env.mockExamRepository.answer(id, questions[26].question.id, setOf("B"))
        env.mockExamRepository.submit(id)

        val vm = MockExamResultViewModel(env.mockExamRepository, env.quizRepository, SavedStateHandle(mapOf("sessionId" to id)))
        val result = vm.uiState.first { it.result != null }.result!!
        assertEquals(26, result.correct)
        assertTrue(result.passed)

        vm.events.test {
            vm.startWeaknessReview()
            val event = awaitItem() as MockExamResultEvent.NavigateToQuiz
            assertEquals(listOf(questions[26].question.id), env.quizRepository.getSessionById(event.sessionId)!!.questionIds)
        }
    }
}
