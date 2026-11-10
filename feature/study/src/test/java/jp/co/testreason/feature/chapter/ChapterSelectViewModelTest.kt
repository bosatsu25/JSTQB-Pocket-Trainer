package jp.co.testreason.feature.chapter

import app.cash.turbine.test
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
class ChapterSelectViewModelTest {

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
    fun overviewCountsChapterQuestions_andStartChapterNavigates() = runTest {
        env.answer("old", SampleData.single1.id, correct = true)
        val vm = ChapterSelectViewModel(env.quizRepository, env.masteryRepository)

        val chapters = vm.uiState.first { it.chapters.isNotEmpty() && it.chapters[0].questionCount == 2 }.chapters
        assertEquals(1, chapters[0].unseenCount)
        assertEquals(1, chapters.single { it.chapter == 4 }.questionCount)

        vm.events.test {
            vm.startChapter(1, count = null)
            val event = awaitItem() as ChapterSelectEvent.NavigateToQuiz
            val session = env.quizRepository.getSessionById(event.sessionId)!!
            assertEquals(StudyMode.CHAPTER, session.mode)
            assertEquals(listOf(SampleData.single2.id, SampleData.single1.id), session.questionIds)
        }
    }
}
