package jp.co.testreason.feature.result

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import jp.co.testreason.core.model.StudyMode
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
class ResultViewModelTest {

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

    private suspend fun viewModel(): ResultViewModel {
        val vm = ResultViewModel(env.quizRepository, SavedStateHandle(mapOf("sessionId" to "s1")))
        vm.uiState.first { it.attempts.size == 2 }
        return vm
    }

    @Test
    fun lowScore_recommendsAndStartsAWeaknessSession() = runTest {
        env.answer("s1", SampleData.single1.id, correct = true)
        env.answer("s1", SampleData.single2.id, correct = false)
        val vm = viewModel()

        assertEquals(50, vm.uiState.value.accuracyPercentage)
        assertEquals(RecommendedAction.WEAKNESS, vm.uiState.value.recommendedAction)
        assertEquals(listOf("FL-1.3.1"), vm.uiState.value.summary.reviewObjectives.map { it.id })

        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startRecommendedSession()
            val event = awaitItem() as ResultUiEvent.NavigateToQuiz
            assertEquals(StudyMode.WEAKNESS, env.quizRepository.getSessionById(event.sessionId)!!.mode)
        }
    }

    @Test
    fun highScore_recommendsTheDailyMix() = runTest {
        env.answer("s1", SampleData.single1.id, correct = true)
        env.answer("s1", SampleData.single2.id, correct = true)
        val vm = viewModel()

        assertEquals(RecommendedAction.DAILY, vm.uiState.value.recommendedAction)
        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startRecommendedSession()
            val event = awaitItem() as ResultUiEvent.NavigateToQuiz
            assertEquals(StudyMode.DAILY, env.quizRepository.getSessionById(event.sessionId)!!.mode)
        }
    }

    @Test
    fun recommendedSession_startsOnlyOnce() = runTest {
        env.answer("s1", SampleData.single1.id, correct = true)
        env.answer("s1", SampleData.single2.id, correct = true)
        val vm = viewModel()

        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startRecommendedSession()
            awaitItem()
            assertTrue("locked: this screen is leaving the back stack", vm.uiState.value.isStarting)
            vm.startRecommendedSession()
            expectNoEvents()
        }
    }
}
