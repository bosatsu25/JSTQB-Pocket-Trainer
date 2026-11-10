package jp.co.testreason.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import jp.co.testreason.core.model.GlossaryTerm
import jp.co.testreason.feature.drill.DrillLap
import jp.co.testreason.feature.glossary.GlossaryCardsActions
import jp.co.testreason.feature.glossary.GlossaryCardsContent
import jp.co.testreason.feature.glossary.GlossaryCardsUiState
import jp.co.testreason.feature.glossary.GlossaryContent
import jp.co.testreason.feature.glossary.GlossaryUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Glossary list and flash cards on every [TestDevice]. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1400dp-h1400dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlossaryScreensResponsiveTest(private val device: TestDevice) {

    @get:Rule
    val compose = createComposeRule()

    // Worst case: the longest keyword of the syllabus with a long summary, listed in three chapters.
    private val longTerm = GlossaryTerm(
        "g-061",
        "コラボレーションベースのテストアプローチ",
        listOf(1, 4, 5),
        "関係者が協力して欠陥を作り込まないことに重点を置く進め方であり、ここは上限の六十文字まで書いた要約の例である。"
    )
    private val terms = listOf(longTerm) + (2..30).map { GlossaryTerm("g-%03d".format(it), "用語$it", listOf(1), "用語${it}の要約。") }
    private val lap = DrillLap.start(terms.take(3).map { it.id })

    @Test
    fun list() {
        val host = compose.setResponsiveContent(device) {
            GlossaryContent(uiState = GlossaryUiState(isLoading = false, terms = terms, chapter = 1), onSelectChapter = {}, onOpenCards = {})
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("全章（30）").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("第6章（0）").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("カードで確認する（30語）").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(longTerm.term).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("用語30の要約。").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun list_emptyChapter_disablesTheCardButton() {
        compose.setResponsiveContent(device) {
            GlossaryContent(uiState = GlossaryUiState(isLoading = false, terms = terms, chapter = 6), onSelectChapter = {}, onOpenCards = {})
        }
        compose.assertNoHorizontalOverflow()
        compose.onNodeWithText("カードで確認する（0語）").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun card_faceDown() {
        val host = compose.setResponsiveContent(device) {
            GlossaryCardsContent(uiState = GlossaryCardsUiState(isLoading = false, lap = lap, term = longTerm), actions = GlossaryCardsActions())
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("用語カード 0 / 3").assertIsDisplayed()
        compose.onNodeWithText("第1章・第4章・第5章").assertIsDisplayed()
        compose.onNodeWithText(longTerm.term).assertIsDisplayed()
        compose.onNodeWithText(longTerm.summary).assertDoesNotExist()
        compose.onNodeWithText("意味を見る").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun card_faceUp_thenTheNextCardStartsAtTheTop() {
        var state by mutableStateOf(GlossaryCardsUiState(isLoading = false, lap = lap, term = longTerm, isRevealed = true))
        val host = compose.setResponsiveContent(device) { GlossaryCardsContent(uiState = state, actions = GlossaryCardsActions()) }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText(longTerm.summary).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("まだ").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("覚えた").performScrollTo().assertIsDisplayed()

        state = GlossaryCardsUiState(isLoading = false, lap = lap.answered(correct = true), term = terms[1])
        compose.waitForIdle()
        compose.onNodeWithText("用語2").assertIsDisplayed()
    }

    @Test
    fun summary() {
        val finished = lap.answered(correct = false).answered(correct = true).answered(correct = true).answered(correct = true)
        val host = compose.setResponsiveContent(device) {
            GlossaryCardsContent(uiState = GlossaryCardsUiState(isLoading = false, lap = finished), actions = GlossaryCardsActions())
        }
        compose.assertNoHorizontalOverflow()
        compose.assertTouchTargetsAtLeast48dp(host)
        compose.onNodeWithText("3語中、1回目で覚えていた語 2語").performScrollTo().assertIsDisplayed()
        listOf("「まだ」の1語だけもう1周", "同じ語でもう1周", "用語一覧へ戻る", "ホームへ戻る").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun devices() = TestDevice.parameters()
    }
}
