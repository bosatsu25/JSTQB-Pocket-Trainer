package jp.co.testreason.feature.home

import app.cash.turbine.test
import jp.co.testreason.feature.testing.MainDispatcherRule
import jp.co.testreason.feature.testing.REPOSITORY_IO_TIMEOUT
import jp.co.testreason.feature.testing.SampleData
import jp.co.testreason.feature.testing.TestEnvironment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

    private fun viewModel() = HomeViewModel(env.quizRepository, env.masteryRepository)

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
    fun dailyQuiz_navigates_andUnfinishedSessionCanBeResumed() = runTest {
        val vm = viewModel()
        lateinit var sessionId: String
        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startDailyQuiz()
            sessionId = (awaitItem() as HomeUiEvent.NavigateToQuiz).sessionId
        }

        vm.uiState.first { it.resumableSession?.id == sessionId }
        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.resumeSession()
            assertEquals(HomeUiEvent.NavigateToQuiz(sessionId), awaitItem())
        }
    }
}
