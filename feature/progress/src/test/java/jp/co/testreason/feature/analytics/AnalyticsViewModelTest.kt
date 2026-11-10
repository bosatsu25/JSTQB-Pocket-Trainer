package jp.co.testreason.feature.analytics

import app.cash.turbine.test
import jp.co.testreason.core.model.KLevel
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
class AnalyticsViewModelTest {

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

    @Test
    fun dashboardFollowsAnswers_andLoSessionStartsFromTheList() = runTest {
        env.answer("s0", SampleData.multi.id, correct = false)
        env.masteryRepository.updateMastery("FL-4.2.2", isCorrect = false, confidence = null)
        val vm = AnalyticsViewModel(env.quizRepository, env.masteryRepository)

        val state = vm.uiState.first { s -> s.kLevels.any { it.kLevel == KLevel.K3 && it.answered == 1 } }
        val lo = state.chapters.single { it.chapter == 4 }.objectives.single { it.id == "FL-4.2.2" }
        assertEquals(ObjectiveState.WEAK, lo.state)
        assertEquals(1, lo.questionCount)

        vm.events.test(timeout = REPOSITORY_IO_TIMEOUT) {
            vm.startObjective("FL-4.2.2")
            val event = awaitItem() as AnalyticsEvent.NavigateToQuiz
            val session = env.quizRepository.getSessionById(event.sessionId)!!
            assertEquals(StudyMode.CUSTOM, session.mode)
            assertEquals(listOf(SampleData.multi.id), session.questionIds)

            assertTrue(vm.uiState.value.isStarting)
            vm.startObjective("FL-1.1.1")
            expectNoEvents()
            env.clock.now += 1
            vm.onScreenShown()
            vm.startObjective("FL-1.1.1")
            awaitItem()
        }
    }
}
