package jp.co.testreason.feature.drill

import androidx.lifecycle.SavedStateHandle
import jp.co.testreason.core.model.Question
import jp.co.testreason.feature.testing.MainDispatcherRule
import jp.co.testreason.feature.testing.SampleData
import jp.co.testreason.feature.testing.TestEnvironment
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DrillViewModelTest {

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

    private suspend fun drill(chapter: Int = 0, count: Int = 0, handle: SavedStateHandle? = null): DrillViewModel {
        val state = handle ?: SavedStateHandle(mapOf("chapter" to chapter, "count" to count))
        return DrillViewModel(env.quizRepository, env.clock, state).also { vm -> vm.uiState.first { !it.isLoading } }
    }

    private val DrillViewModel.state get() = uiState.value
    private val DrillViewModel.question: Question get() = uiState.value.question!!

    private fun DrillViewModel.answer(correct: Boolean) {
        val q = question
        val ids = if (correct) q.correctChoiceIds else listOf(q.choices.first { !it.isCorrect }.id)
        // A wrong multi-answer needs the required number of choices: one wrong plus correct ones.
        val picks = if (correct) ids else ids + q.correctChoiceIds.take(q.selectCount - 1)
        picks.forEach(::select)
    }

    /** "次へ", then wait out the accidental-tap guard like a learner reading the next question would. */
    private fun DrillViewModel.goNext() {
        next()
        env.clock.now += 1_000
    }

    @Test
    fun singleAnswer_isRevealedOnTheTap_andAMissComesBackUntilAnsweredCorrectly() = runTest {
        val vm = drill(chapter = 1)
        assertEquals(2, vm.state.lap.total)
        val first = vm.question

        vm.answer(correct = false)
        assertEquals(DrillReveal.WRONG, vm.state.reveal)
        assertEquals(first, vm.question) // the answer is shown on the same question
        vm.goNext()

        val second = vm.question
        assertNotEquals(first.id, second.id)
        assertNull(vm.state.reveal)
        assertTrue(vm.state.selectedChoiceIds.isEmpty())
        vm.answer(correct = true)
        assertEquals(DrillReveal.CORRECT, vm.state.reveal)
        vm.goNext()

        assertEquals(first.id, vm.question.id) // the missed question is asked again
        assertFalse(vm.state.isFinished)
        vm.answer(correct = true)
        vm.goNext()

        assertTrue(vm.state.isFinished)
        assertEquals(setOf(first.id), vm.state.lap.missed)
        assertEquals(1, vm.state.lap.firstTryCorrectCount)
        assertEquals(3, vm.state.lap.answerCount)
    }

    @Test
    fun multiAnswer_isRevealedOnlyWhenTheRequiredNumberIsChosen() = runTest {
        val vm = drill(chapter = 4)
        assertEquals(SampleData.multi.id, vm.question.id)

        vm.select("B")
        assertNull(vm.state.reveal)
        vm.select("B") // deselect
        assertTrue(vm.state.selectedChoiceIds.isEmpty())
        vm.select("B")
        vm.select("D")

        assertEquals(DrillReveal.CORRECT, vm.state.reveal)
        assertEquals(setOf("B", "D"), vm.state.selectedChoiceIds)
    }

    @Test
    fun choicesAreLockedOnceTheAnswerIsShown() = runTest {
        val vm = drill(chapter = 4)
        vm.answer(correct = false)
        val selected = vm.state.selectedChoiceIds

        vm.select("E")
        vm.showAnswer()

        assertEquals(selected, vm.state.selectedChoiceIds)
        assertEquals(DrillReveal.WRONG, vm.state.reveal)
    }

    @Test
    fun showAnswer_countsAsAMiss_andNextIsIgnoredBeforeAnAnswer() = runTest {
        val vm = drill(chapter = 4)

        vm.next() // nothing answered yet
        assertEquals(0, vm.state.lap.answerCount)

        vm.showAnswer()
        assertEquals(DrillReveal.SHOWN, vm.state.reveal)
        vm.next()
        vm.next() // double tap on "次へ"

        assertEquals(setOf(SampleData.multi.id), vm.state.lap.missed)
        assertEquals(1, vm.state.lap.answerCount)
        assertFalse(vm.state.isFinished)
    }

    @Test
    fun secondTapOfADoubleTapOnNext_doesNotAnswerTheNextQuestion() = runTest {
        val vm = drill(chapter = 1)
        vm.answer(correct = true)
        vm.next()

        env.clock.now += 100
        vm.answer(correct = false) // lands on a choice of the next question
        vm.showAnswer()
        assertNull(vm.state.reveal)
        assertTrue(vm.state.selectedChoiceIds.isEmpty())

        env.clock.now += 300
        vm.answer(correct = true)
        assertEquals(DrillReveal.CORRECT, vm.state.reveal)
    }

    @Test
    fun reasonsAreOptIn_andCloseAgainOnTheNextQuestion() = runTest {
        val vm = drill(chapter = 1)
        vm.answer(correct = true)
        assertFalse(vm.state.showReasons)

        vm.toggleReasons()
        assertTrue(vm.state.showReasons)
        vm.goNext()

        assertFalse(vm.state.showReasons)
    }

    @Test
    fun restart_canRepeatOnlyTheMissedQuestions() = runTest {
        val vm = drill(chapter = 1)
        val missed = vm.question.id
        vm.answer(correct = false)
        vm.goNext()
        repeat(2) {
            vm.answer(correct = true)
            vm.goNext()
        }
        assertTrue(vm.state.isFinished)

        vm.restart(onlyMissed = true)
        assertEquals(listOf(missed), vm.state.lap.questionIds)
        assertEquals(0, vm.state.lap.answerCount)
        assertTrue(vm.state.lap.missed.isEmpty())
        vm.answer(correct = true)
        vm.goNext()
        assertTrue(vm.state.isFinished)

        vm.restart(onlyMissed = false)
        assertEquals(1, vm.state.lap.total)
        assertFalse(vm.state.isFinished)
    }

    @Test
    fun chapterAndCount_limitTheLap() = runTest {
        assertEquals(3, drill().state.lap.total)
        assertEquals(1, drill(chapter = 1, count = 1).state.lap.total)
        assertTrue(drill(chapter = 1, count = 1).question.learningObjectiveId.startsWith("FL-1."))
        assertTrue(drill(chapter = 6).state.isEmpty)
    }

    @Test
    fun finishedLap_reportsThePace() = runTest {
        val vm = drill(chapter = 1)
        repeat(2) {
            env.clock.now += 6_000
            vm.answer(correct = true)
            vm.next()
        }
        assertEquals(12_000L, vm.state.elapsedMs)
        assertEquals(6, vm.state.secondsPerAnswer)
    }

    @Test
    fun drill_writesNoAttemptMasteryReviewOrSession() = runTest {
        val vm = drill()
        vm.answer(correct = false)
        vm.goNext()
        while (!vm.state.isFinished) {
            vm.answer(correct = true)
            vm.goNext()
        }

        assertTrue(env.quizRepository.observeAllAttempts().first().isEmpty())
        assertTrue(env.masteryRepository.getAllMasteries().first().isEmpty())
        assertTrue(env.quizRepository.observeWeakQuestions().first().isEmpty())
        assertTrue(env.quizRepository.observeDueReviewQuestions().first().isEmpty())
        assertNull(env.quizRepository.getActiveSession().first())
    }

    @Test
    fun lapSurvivesProcessDeath() = runTest {
        val handle = SavedStateHandle(mapOf("chapter" to 1, "count" to 0))
        val before = drill(handle = handle)
        before.answer(correct = false)
        before.next()
        env.clock.now += 5_000

        val after = drill(handle = handle)

        assertEquals(before.state.lap, after.state.lap)
        assertEquals(before.question.id, after.question.id)
        after.answer(correct = true)
        after.goNext()
        after.answer(correct = true)
        after.goNext()
        assertEquals(6_000L, after.state.elapsedMs) // the clock keeps running from the original start
    }

    @Test
    fun setup_listsQuestionCountsPerChapter() = runTest {
        val vm = DrillSetupViewModel(env.quizRepository)
        val state = vm.uiState.first { it.totalCount == 3 }
        assertEquals(6, state.chapters.size)
        assertEquals(2, state.chapters.single { it.chapter == 1 }.questionCount)
        assertEquals(1, state.chapters.single { it.chapter == 4 }.questionCount)
        assertEquals(0, state.chapters.single { it.chapter == 6 }.questionCount)
    }
}
