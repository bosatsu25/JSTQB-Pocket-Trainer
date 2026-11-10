package jp.co.testreason.feature.review

import app.cash.turbine.test
import jp.co.testreason.core.database.ReviewScheduleEntity
import jp.co.testreason.core.model.StudyMode
import jp.co.testreason.feature.testing.MainDispatcherRule
import jp.co.testreason.feature.testing.SampleData
import jp.co.testreason.feature.testing.TestEnvironment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewViewModelTest {

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
    fun dueQuestions_areListed_andStartCreatesAReviewSession() = runTest {
        env.db.reviewScheduleDao().insertOrUpdateSchedule(ReviewScheduleEntity(SampleData.single2.id, env.clock.now - 1, 0L, 1))
        env.db.reviewScheduleDao().insertOrUpdateSchedule(ReviewScheduleEntity(SampleData.single1.id, env.clock.now + 60_000, 0L, 1))
        val vm = ReviewViewModel(env.quizRepository)

        val state = vm.uiState.first { !it.isLoading }
        assertEquals(listOf(SampleData.single2.id), state.dueQuestions.map { it.id })

        vm.events.test {
            vm.startDueReviewSession()
            val event = awaitItem() as ReviewUiEvent.NavigateToQuiz
            assertEquals(StudyMode.REVIEW, env.quizRepository.getSessionById(event.sessionId)!!.mode)
        }
    }

    @Test
    fun emptyQueue_startsNothing() = runTest {
        val vm = ReviewViewModel(env.quizRepository)
        assertEquals(0, vm.uiState.first { !it.isLoading }.dueQuestions.size)

        vm.events.test {
            vm.startDueReviewSession()
            expectNoEvents()
        }
        assertEquals(null, env.quizRepository.getActiveSession().first())
    }
}
