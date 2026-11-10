package jp.co.testreason.feature.glossary

import androidx.lifecycle.SavedStateHandle
import jp.co.testreason.core.content.GlossarySource
import jp.co.testreason.core.model.GlossaryTerm
import jp.co.testreason.feature.testing.MainDispatcherRule
import jp.co.testreason.feature.testing.MutableClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GlossaryViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val clock = MutableClock()
    private val terms = listOf(
        GlossaryTerm("g-001", "カバレッジ", listOf(1, 4), "割合。"),
        GlossaryTerm("g-002", "欠陥", listOf(1), "誤った部分。"),
        GlossaryTerm("g-003", "境界値分析", listOf(4), "境目を狙う技法。")
    )
    private val source = GlossarySource { terms }

    private suspend fun cards(chapter: Int = 0, handle: SavedStateHandle? = null): GlossaryCardsViewModel {
        val state = handle ?: SavedStateHandle(mapOf("chapter" to chapter))
        return GlossaryCardsViewModel(source, clock, state).also { vm -> vm.uiState.first { !it.isLoading } }
    }

    private val GlossaryCardsViewModel.state get() = uiState.value

    /** Turn the card, read it, then say whether it was remembered. */
    private fun GlossaryCardsViewModel.turnAndGrade(remembered: Boolean) {
        reveal()
        clock.now += 1_000
        grade(remembered)
    }

    @Test
    fun list_filtersByChapter_andATermOfTwoChaptersIsInBoth() = runTest {
        val vm = GlossaryViewModel(source)
        val loaded = vm.uiState.first { !it.isLoading }
        assertEquals(3, loaded.visibleTerms.size)
        assertEquals(2, loaded.countIn(1))
        assertEquals(2, loaded.countIn(4))
        assertEquals(0, loaded.countIn(6))

        vm.selectChapter(4)
        assertEquals(listOf("カバレッジ", "境界値分析"), vm.uiState.value.visibleTerms.map { it.term })
        vm.selectChapter(null)
        assertEquals(3, vm.uiState.value.visibleTerms.size)
    }

    @Test
    fun cards_aTermMarkedNotYetComesBack_untilRemembered() = runTest {
        val vm = cards(chapter = 1)
        assertEquals(2, vm.state.lap.total)
        val first = vm.state.term!!
        assertFalse(vm.state.isRevealed)

        vm.turnAndGrade(remembered = false)
        assertNotEquals(first.id, vm.state.term!!.id)
        assertFalse(vm.state.isRevealed) // the next card starts face down
        vm.turnAndGrade(remembered = true)

        assertEquals(first.id, vm.state.term!!.id)
        vm.turnAndGrade(remembered = true)

        assertTrue(vm.state.isFinished)
        assertEquals(setOf(first.id), vm.state.lap.missed)
        assertEquals(1, vm.state.lap.firstTryCorrectCount)
    }

    @Test
    fun cards_cannotBeGradedFaceDown_orByTheSecondTapOfADoubleTap() = runTest {
        val vm = cards()
        vm.grade(remembered = true) // face down
        assertEquals(0, vm.state.lap.answerCount)

        vm.reveal()
        clock.now += 100
        vm.grade(remembered = true) // double tap on "意味を見る"
        assertEquals(0, vm.state.lap.answerCount)
        assertTrue(vm.state.isRevealed)

        clock.now += 300
        vm.grade(remembered = true)
        assertEquals(1, vm.state.lap.answerCount)
    }

    @Test
    fun cards_restartCanRepeatOnlyTheMissedTerms() = runTest {
        val vm = cards()
        val missed = vm.state.term!!.id
        vm.turnAndGrade(remembered = false)
        repeat(3) { vm.turnAndGrade(remembered = true) }
        assertTrue(vm.state.isFinished)

        vm.restart(onlyMissed = true)
        assertEquals(listOf(missed), vm.state.lap.questionIds)
        vm.turnAndGrade(remembered = true)
        vm.restart(onlyMissed = false)
        assertEquals(1, vm.state.lap.total)
        assertFalse(vm.state.isFinished)
    }

    @Test
    fun cards_emptyChapter_andProcessDeath() = runTest {
        assertTrue(cards(chapter = 6).state.isEmpty)

        val handle = SavedStateHandle(mapOf("chapter" to 0))
        val before = cards(handle = handle)
        before.turnAndGrade(remembered = false)
        val after = cards(handle = handle)
        assertEquals(before.state.lap, after.state.lap)
        assertEquals(before.state.term, after.state.term)
    }
}
