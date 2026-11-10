package jp.co.testreason.feature.home

import app.cash.turbine.test
import jp.co.testreason.core.data.QuizRepository
import jp.co.testreason.core.model.QuizSession
import jp.co.testreason.feature.testing.MainDispatcherRule
import jp.co.testreason.feature.testing.REPOSITORY_IO_TIMEOUT
import jp.co.testreason.feature.testing.SampleData
import jp.co.testreason.feature.testing.TestEnvironment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {

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

    private fun viewModel() = HomeViewModel(env.quizRepository, env.masteryRepository, env.preferences)

    @Test
    fun weaknessReview_withNoWeakQuestions_showsAMessageInsteadOfAnEmptySession() = runTest {
        val vm = viewModel()
        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startWeaknessReview()
            assertTrue(awaitItem() is HomeUiEvent.ShowMessage)
        }
        assertEquals(null, env.quizRepository.getActiveSession().first())
    }

    @Test
    fun weakCount_andWeaknessSession_followWrongAnswers() = runTest {
        env.answer("old", SampleData.single2.id, correct = false)
        val vm = viewModel()
        vm.uiState.first { it.weakQuestionCount == 1 }

        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startWeaknessReview()
            val event = awaitItem() as HomeUiEvent.NavigateToQuiz
            assertEquals(listOf(SampleData.single2.id), env.quizRepository.getSessionById(event.sessionId)!!.questionIds)
        }
    }

    @Test
    fun dailyQuiz_usesTheDailyGoalSetting() = runTest {
        env.close()
        env = TestEnvironment(SampleData.examBank()) // enough questions for the larger goals
        env.start()
        val vm = viewModel()
        assertEquals(5, vm.uiState.value.dailyGoal)
        assertEquals(3, vm.uiState.value.dailyMinutes)

        env.preferences.setDailyGoal(10)
        val state = vm.uiState.first { it.dailyGoal == 10 }
        assertEquals(6, state.dailyMinutes)

        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startDailyQuiz()
            val event = awaitItem() as HomeUiEvent.NavigateToQuiz
            assertEquals(10, env.quizRepository.getSessionById(event.sessionId)!!.questionIds.size)
        }
    }

    @Test
    fun dailyQuiz_navigates_andUnfinishedSessionCanBeResumed() = runTest {
        val vm = viewModel()
        lateinit var sessionId: String
        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startDailyQuiz()
            sessionId = (awaitItem() as HomeUiEvent.NavigateToQuiz).sessionId
        }

        vm.uiState.first { it.resumableSession?.id == sessionId }
        vm.onScreenShown() // the learner comes back to Home
        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.resumeSession()
            assertEquals(HomeUiEvent.NavigateToQuiz(sessionId), awaitItem())
        }
    }

    @Test
    fun startButtons_stayLockedAfterASessionStarted_untilHomeIsShownAgain() = runTest {
        val vm = viewModel()
        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startDailyQuiz()
            val first = (awaitItem() as HomeUiEvent.NavigateToQuiz).sessionId
            assertTrue("locked while navigating away", vm.uiState.value.isLoading)

            vm.startDailyQuiz()
            vm.startWeaknessReview()
            vm.resumeSession()
            expectNoEvents()

            env.clock.now += 1
            vm.onScreenShown()
            assertFalse(vm.uiState.value.isLoading)
            vm.startDailyQuiz()
            assertNotEquals(first, (awaitItem() as HomeUiEvent.NavigateToQuiz).sessionId)
        }
    }

    @Test
    fun returningToHomeWhileASessionIsStillBeingCreated_keepsTheLock() = runTest {
        // Home's other menu entries stay usable while a session is created, so Home can be shown again in between.
        val gate = CompletableDeferred<Unit>()
        val slow = object : QuizRepository by env.quizRepository {
            override suspend fun createDailySession(count: Int): QuizSession? {
                gate.await()
                return env.quizRepository.createDailySession(count)
            }
        }
        val vm = HomeViewModel(slow, env.masteryRepository, env.preferences)

        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startDailyQuiz()
            vm.onScreenShown()
            assertTrue("still creating: must stay locked", vm.uiState.value.isLoading)
            vm.startDailyQuiz()
            vm.startWeaknessReview()

            gate.complete(Unit)
            awaitItem()
            assertTrue("navigating: still locked", vm.uiState.value.isLoading)

            vm.onScreenShown()
            assertFalse(vm.uiState.value.isLoading)
        }
    }
}

